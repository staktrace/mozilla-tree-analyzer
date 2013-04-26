import java.io.*;
import java.math.*;
import java.util.*;
import com.staktrace.util.conv.json.*;

class BuildTimeHistory {
    public static void main( String[] args ) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java BuildTimeHistory <builder-name> <raw-build-folder>");
            return;
        }

        File folder = new File( args[1] );
        System.out.println( "Datafile, Total Time, Number of jobs, Average Time" );
        for (String rawfile : folder.list()) {
            System.err.println( "Loading raw data from file " + rawfile );
            Map<Integer, String> builderNames = new HashMap<Integer, String>();

            FileReader fr = new FileReader( new File( folder, rawfile ) );
            JsonReader jr = new JsonReader( fr );
            JsonObject root = jr.readObject();

            JsonObject builders = (JsonObject)root.getValue( "builders" );
            Set<Integer> builderIds = new HashSet<Integer>();
            for (int i = builders.size() - 1; i >= 0; i--) {
                JsonObject builder = (JsonObject)builders.getValue( i );
                if (args[0].equals( builder.getValue( "name" ) )) {
                    builderIds.add( Integer.parseInt( builders.getKey( i ) ) );
                    break;
                }
            }

            JsonArray builds = (JsonArray)root.getValue( "builds" );
            long totalTime = 0;
            int count = 0;
            for (int i = builds.size() - 1; i >= 0; i--) {
                JsonObject build = (JsonObject)builds.getValue( i );
                if (!builderIds.contains( ( (BigInteger)build.getValue( "builder_id" ) ).intValue() )) {
                    continue;
                }
                BigInteger startTime = (BigInteger)build.getValue( "starttime" );
                BigInteger endTime = (BigInteger)build.getValue( "endtime" );
                if (startTime == null || endTime == null) {
                    continue;
                }
                long time = (endTime.longValue() - startTime.longValue());
                totalTime += time;
                count++;
            }

            System.out.println( rawfile + ", " + totalTime + ", " + count + ", " + (count == 0 ? 0 : totalTime / count) );
            fr.close();
        }
    }
}
