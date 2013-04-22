import java.io.*;
import java.math.*;
import java.util.*;
import com.staktrace.util.conv.json.*;

class BuildTimes {
    public static void main( String[] args ) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java BuildTimes <pushes-file> <raw-build-folder>");
            return;
        }

        List<String> pushrevs = new ArrayList<String>();
        Set<String> allbuilders = new TreeSet<String>();
        Map<String, Map<String, Long>> buildtimes = new HashMap<String, Map<String, Long>>();

        System.err.println( "Loading pushes..." );
        BufferedReader br = new BufferedReader( new FileReader( args[0] ) );
        for (String s = br.readLine(); s != null; s = br.readLine()) {
            String pushrev = s.substring( 0, 12 );
            pushrevs.add( pushrev );
            buildtimes.put( pushrev, new HashMap<String, Long>() );
        }
        br.close();

        File folder = new File( args[1] );
        for (String rawfile : folder.list()) {
            System.err.println( "Loading raw data from file " + rawfile );
            Map<Integer, String> builderNames = new HashMap<Integer, String>();

            FileReader fr = new FileReader( new File( folder, rawfile ) );
            JsonReader jr = new JsonReader( fr );
            JsonObject root = jr.readObject();

            JsonObject builders = (JsonObject)root.getValue( "builders" );
            for (int i = builders.size() - 1; i >= 0; i--) {
                JsonObject builder = (JsonObject)builders.getValue( i );
                if (! "mozilla-inbound".equals( builder.getValue( "category" ) )) {
                    continue;
                }
                String builderName = (String)builder.getValue( "name" );
                builderNames.put( Integer.parseInt( builders.getKey( i ) ), builderName );
            }
            
            JsonArray builds = (JsonArray)root.getValue( "builds" );
            for (int i = builds.size() - 1; i >= 0; i--) {
                JsonObject build = (JsonObject)builds.getValue( i );
                BigInteger builderId = (BigInteger)build.getValue( "builder_id" );
                String builderName = builderNames.get( builderId.intValue() );
                if (builderName == null) {
                    continue;
                }
                Object props = build.getValue( "properties" );
                if (props instanceof JsonObject) {
                    JsonObject properties = (JsonObject)props;
                    String revision = (String)properties.getValue( "revision" );
                    if (revision == null) {
                        System.err.println( "Missing revision for build id " + build.getValue( "id" ) );
                        continue;
                    }
                    if (revision.length() < 12) {
                        System.err.println( "Invalid revision for build id " + build.getValue( "id" ) );
                        continue;
                    }
                    revision = revision.substring( 0, 12 );
                    Map<String, Long> durations = buildtimes.get( revision );
                    if (durations == null) {
                        continue;
                    }
                    Object start = build.getValue( "starttime" );
                    Object end = build.getValue( "endtime" );
                    if (start instanceof BigInteger && end instanceof BigInteger) {
                        allbuilders.add( builderName );
                        long newduration = ( (BigInteger)end ).subtract( (BigInteger)start ).longValue();
                        Long oldduration = durations.get( builderName );
                        durations.put( builderName, newduration + (oldduration == null ? 0 : oldduration.longValue()) );
                    } else {
                        System.err.println( "Missing time info for build id " + build.getValue( "id" ) );
                        continue;
                    }
                } else {
                    System.err.println( "Missing properties for build id " + build.getValue( "id" ) );
                    continue;
                }
            }

            fr.close();
        }

        System.out.print("Revision");
        for (String builder : allbuilders) {
            System.out.print("," + builder);
        }
        System.out.println();
        for (String pushrev : pushrevs) {
            Map<String, Long> times = buildtimes.get( pushrev );
            System.out.print( pushrev );
            for (String builder : allbuilders) {
                Long duration = times.get( builder );
                System.out.print("," + (duration == null ? "" : duration));
            }
            System.out.println();
        }
    }
}
