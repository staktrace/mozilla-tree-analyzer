import java.io.*;
import java.util.*;
import com.staktrace.util.conv.csv.*;

class CoalesceStats {
    public static void main( String[] args ) throws Exception {
        if (args.length == 0) {
            System.err.println( "Usage: java CoalesceStats <time-table-file> <no-backout-list-file>" );
            return;
        }

        Set<String> noBackoutCsets = new HashSet<String>();
        BufferedReader br = new BufferedReader( new FileReader( args[1] ) );
        for (String s = br.readLine(); s != null; s = br.readLine()) {
            noBackoutCsets.add( s.substring( 0, 12 ) );
        }
        br.close();

        CsvReader cr = new CsvReader( new FileReader( args[0] ) );

        List<String> builds = cr.readLine();
        if (! "Revision".equals( builds.get( 0 ) )) {
            throw new Exception( "Unexpected first line" );
        }
        builds.remove( 0 );

        Map<String, Map<String, Long>> buildTimes = new HashMap<String, Map<String, Long>>();
        for (List<String> fields = cr.readLine(); fields != null; fields = cr.readLine()) {
            String rev = fields.get( 0 );
            Map<String, Long> times = new HashMap<String, Long>();
            buildTimes.put( rev, times );
            for (int i = 0; i < builds.size(); i++) {
                String time = fields.get( i + 1 );
                long duration = (time.length() == 0 ? -1 : Long.parseLong( time ));
                times.put( builds.get( i ), duration );
            }
        }

        long actualTime = 0;
        long uncoalescedTime = 0;
        long noBackoutsTime = 0;
        long noBackoutsUncoalescedTime = 0;

        int actualCount = 0;
        int uncoalescedCount = 0;
        int noBackoutsCount = 0;
        int noBackoutsUncoalescedCount = 0;

        int buildersSkipped = 0;
        Map<String, Long> avgTime = new HashMap<String, Long>();
        for (String build : builds) {
            if (build.indexOf( "_pgo" ) >= 0 || build.indexOf( "-pgo" ) >= 0) {
                buildersSkipped++;
                continue;
            }
            long total = 0;
            int count = 0;
            for (String rev : buildTimes.keySet()) {
                Map<String, Long> times = buildTimes.get( rev );
                long time = times.get( build );
                if (time >= 0) {
                    total += time;
                    count++;
                }
            }
            long avg = total / count;
            if (count * 2 < buildTimes.size()) {
                System.out.println( "Warning: number of " + build + " builds is " + count + "; less than half the uncoalesced value (" + buildTimes.size() + ")" );
            }

            for (String rev : buildTimes.keySet()) {
                Map<String, Long> times = buildTimes.get( rev );
                long time = times.get( build );
                if (time < 0) {
                    time = avg;
                    times.put( build, time );
                } else {
                    actualTime += time;
                    actualCount++;
                    if (noBackoutCsets.contains( rev )) {
                        noBackoutsTime += time;
                        noBackoutsCount++;
                    }
                }

                uncoalescedTime += time;
                uncoalescedCount++;
                if (noBackoutCsets.contains( rev )) {
                    noBackoutsUncoalescedTime += time;
                    noBackoutsUncoalescedCount++;
                }
            }
        }

        System.out.println( "Skipped " + buildersSkipped + " builders due to PGO" );
        System.out.println( "Actual time taken: " + actualTime + ", in " + actualCount + " builds");
        System.out.println( "w/o coalescing: " + uncoalescedTime + " (" + percent(actualTime, uncoalescedTime) + "%), in " + uncoalescedCount + " builds");
        System.out.println( "w/o backouts: " + noBackoutsTime + " (" + percent(actualTime, noBackoutsTime) + "%), in " + noBackoutsCount + " builds" );
        System.out.println( "w/o backouts or coalescing: " + noBackoutsUncoalescedTime + " (" + percent(actualTime, noBackoutsUncoalescedTime) + "%), in " + noBackoutsUncoalescedCount + " builds" );
    }

    private static float percent( long base, long val ) {
        return (100 * (val - base) / base);
    }
}
