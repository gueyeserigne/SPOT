package cetud;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.util.*;

public class Statistics {
	
	// -------------------- ACTIVITY STATS --------------------
    static class ActivityStatsCollector implements ActivityStartEventHandler, ActivityEndEventHandler {
        private final Map<String, Integer> activityCounts = new HashMap<>();
    	private final Map<String, Map <Integer, Integer>> activityCounts_time = new HashMap<>();

        @Override
        public void handleEvent(ActivityStartEvent event) {
            activityCounts.merge(event.getActType(), 1, Integer::sum);        	
        	Map <Integer, Integer> actType = activityCounts_time.get(event.getActType());
        	if(actType == null) {
        		actType = new HashMap();
        		activityCounts_time.put(event.getActType(), actType);
        	}
        	
        	//System.out.println("Event Type " + event.getActType() + " " + event.getEventType());
        	actType.merge((int) event.getTime(), 1, Integer::sum);
        	if(event.getActType().compareTo("DEPART_HOME") == 0)
        		System.out.println("Event Type " + event.getActType() + " " + event.getEventType() + " " + event.getTime());
            
        }

        @Override
        public void handleEvent(ActivityEndEvent event) {
            // Optional: count ends separately
        	activityCounts.merge(event.getActType(), 1, Integer::sum);
        	Map <Integer, Integer> act = activityCounts_time.get(event.getActType());
        	if(act == null) {
        		act = new HashMap();
        		activityCounts_time.put(event.getActType(), act);
        	}
        		
        	act.merge((int) event.getTime(), 1, Integer::sum);
        	
        	/*
        	double time = event.getTime();
        	Map<String,String> attr = event.getAttributes();
        	
        	System.out.println("Event Type " + event.getActType() + " " + event.getActType());
        	
        	for(String s : attr.keySet()) {
        		System.out.println("Attr " + s + " " + attr.get(s));
        	}
        	*/
        	if(event.getActType().compareTo("DEPART_HOME") == 0)
        		System.out.println("Event Type " + event.getActType() + " " + event.getEventType() + " " + event.getTime());
            
        }

        @Override
        public void reset(int iteration) {
            activityCounts_time.clear();
        }

        public void printStats() {
            System.out.println("Activity counts:");
            activityCounts.forEach((type, count) ->
                    System.out.printf("  %s: %d%n", type, count));
            /*
            for(String type : activityCounts_time.keySet()) {
            	Map <Integer, Integer> act = activityCounts_time.get(type);
            	for(int time : act.keySet()) {
            		System.out.println(type + " " + time + " " + act.get(time));
            	}            	
            }*/
        }

        public void plot(String file) throws Exception {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (String e : activityCounts_time.keySet()) {
            	Map<Integer, Integer> act = activityCounts_time.get(e);
            	for (Integer t : act.keySet()) {
            		//dataset.addValue(e.getValue(), "Activities", e.getKey());	
            		dataset.addValue(act.get(t), e, t);
            	}
                
            }
           // var chart = ChartFactory.createBarChart(
             //       "Activity Counts", "Activity Type", "Count", dataset);
        //    var chart = ChartFactory.createBarChart(
          //         "Number of activities", "Time", "Count", dataset);

            var chart = ChartFactory.createLineChart(
                    "Number of activities", "Time", "Count", dataset);

            ChartUtils.saveChartAsPNG(new File(file), chart, 800, 600);
        }
    }

    // -------------------- TRIP STATS --------------------
    static class TripStatsCollector implements PersonDepartureEventHandler, PersonArrivalEventHandler {
        private final Map<Id<Person>, Double> departures = new HashMap<>();
        private final Map<String, Double> totalTimeByMode = new HashMap<>();
        private final Map<String, Integer> countByMode = new HashMap<>();

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            departures.put(event.getPersonId(), event.getTime());
        }

        @Override
        public void handleEvent(PersonArrivalEvent event) {
            Double depTime = departures.remove(event.getPersonId());
            if (depTime != null) {
                double travelTime = event.getTime() - depTime;
                String mode = event.getLegMode();
                totalTimeByMode.merge(mode, travelTime, Double::sum);
                countByMode.merge(mode, 1, Integer::sum);
            }
        }

        @Override
        public void reset(int iteration) {
            departures.clear();
            totalTimeByMode.clear();
            countByMode.clear();
        }

        public void printStats() {
        	double avg_total = 0;
        	int count_total = 0;
            System.out.println("Trip statistics (avg travel time per mode):");
            for (String mode : totalTimeByMode.keySet()) {
                double avg = totalTimeByMode.get(mode) / countByMode.get(mode);
                avg_total += totalTimeByMode.get(mode);
                count_total += countByMode.get(mode);
                System.out.printf("  %s: %.1f sec (%d trips)%n", mode, avg, countByMode.get(mode));
            }
            
            System.out.printf("  Avg: %.1f sec %n", avg_total/count_total);
        }

        public void plot(String file) throws Exception {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (String mode : totalTimeByMode.keySet()) {
                double avg = totalTimeByMode.get(mode) / countByMode.get(mode);
                dataset.addValue(avg, "Avg Travel Time (s)", mode);
            }
            var chart = ChartFactory.createBarChart(
                    "Trip Statistics", "Mode", "Avg Travel Time (s)", dataset);
            ChartUtils.saveChartAsPNG(new File(file), chart, 800, 600);
        }
    }

    // -------------------- VEHICLE HISTOGRAM --------------------
    static class VehicleHistogramCollector implements PersonDepartureEventHandler {
        private final int binSize;
        private final Map<Integer, Integer> histogram = new TreeMap<>();

        public VehicleHistogramCollector(int binSize) {
            this.binSize = binSize;
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            int bin = (int) (event.getTime() / binSize);
            histogram.merge(bin, 1, Integer::sum);
        }

        @Override
        public void reset(int iteration) {
            histogram.clear();
        }

        public void printHistogram() {
            System.out.println("Vehicle trip histogram:");
            histogram.forEach((bin, count) -> {
                double start = bin * binSize / 3600.0;
                double end = (bin + 1) * binSize / 3600.0;
                System.out.printf("  %.2f–%.2f h: %d departures%n", start, end, count);
            });
        }

        public void plot(String file) throws Exception {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (var e : histogram.entrySet()) {
                String label = String.format("%.1f–%.1fh",
                        e.getKey() * binSize / 3600.0,
                        (e.getKey() + 1) * binSize / 3600.0);
                dataset.addValue(e.getValue(), "Departures", label);
            }
            var chart = ChartFactory.createBarChart(
                    "Vehicle Departures Histogram", "Time Interval", "Departures", dataset);
            ChartUtils.saveChartAsPNG(new File(file), chart, 1000, 600);
        }
    }

    
 // -------------------- TRIP STATS BY MOTIF --------------------
    static class TripByMotifCollector implements BasicEventHandler {

        static class MotifStats {
            int count = 0;
            double totalDurationSeconds = 0.0;

            void add(double durationSeconds) {
                count++;
                totalDurationSeconds += durationSeconds;
            }

            double avgMinutes() {
                return count == 0 ? 0.0 : totalDurationSeconds / count / 60.0;
            }
        }

        static class TripState {
            double departureTime = -1.0;
            double arrivalTime = -1.0;
        }

        private final Map<String, TripState> states = new HashMap<>();
        private final Map<String, MotifStats> statsByMotif = new TreeMap<>();

        @Override
        public void handleEvent(Event event) {
            String type = event.getEventType();
            Map<String, String> attr = event.getAttributes();

            String person = attr.get("person");
            if (person == null) return;

            if ("departure".equals(type)) {
                TripState state = states.computeIfAbsent(person, k -> new TripState());
                state.departureTime = event.getTime();
                state.arrivalTime = -1.0;
            }

            else if ("arrival".equals(type)) {
                TripState state = states.computeIfAbsent(person, k -> new TripState());
                state.arrivalTime = event.getTime();
            }

            else if ("actstart".equals(type)) {
                TripState state = states.get(person);
                if (state == null) return;
                if (state.departureTime < 0 || state.arrivalTime < 0) return;

                String motif = attr.get("actType");
                if (motif == null || motif.contains("interaction")) return;

                double duration = state.arrivalTime - state.departureTime;
                if (duration >= 0) {
                    statsByMotif
                            .computeIfAbsent(motif, k -> new MotifStats())
                            .add(duration);
                }

                state.departureTime = -1.0;
                state.arrivalTime = -1.0;
            }
        }

        @Override
        public void reset(int iteration) {
            states.clear();
            statsByMotif.clear();
        }

        public void printStats() {
            int totalTrips = statsByMotif.values()
                    .stream()
                    .mapToInt(s -> s.count)
                    .sum();

            System.out.println("Distribution des déplacements par motif :");
            System.out.println("Motif ; Nombre ; Distribution (%) ; Durée moyenne (mn)");

            if (totalTrips == 0) {
                System.out.println("Aucun déplacement trouvé.");
                return;
            }

            for (Map.Entry<String, MotifStats> e : statsByMotif.entrySet()) {
                MotifStats s = e.getValue();
                double pct = 100.0 * s.count / totalTrips;

                System.out.printf(
                        Locale.US,
                        "%s ; %d ; %.2f ; %.2f%n",
                        e.getKey(),
                        s.count,
                        pct,
                        s.avgMinutes()
                );
            }
        }

        public void writeCsv(String file) throws Exception {
            int totalTrips = statsByMotif.values()
                    .stream()
                    .mapToInt(s -> s.count)
                    .sum();

            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.OutputStreamWriter(
                            new java.io.FileOutputStream(file),
                            java.nio.charset.StandardCharsets.UTF_8
                    )
            )) {
                pw.println("motif;nombre_deplacements;distribution_pourcentage;duree_moyenne_mn");

                for (Map.Entry<String, MotifStats> e : statsByMotif.entrySet()) {
                    MotifStats s = e.getValue();
                    double pct = totalTrips == 0 ? 0.0 : 100.0 * s.count / totalTrips;

                    pw.printf(
                            Locale.US,
                            "%s;%d;%.2f;%.2f%n",
                            e.getKey(),
                            s.count,
                            pct,
                            s.avgMinutes()
                    );
                }
            }
        }

        public void plot(String file) throws Exception {
            int totalTrips = statsByMotif.values()
                    .stream()
                    .mapToInt(s -> s.count)
                    .sum();

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            for (Map.Entry<String, MotifStats> e : statsByMotif.entrySet()) {
                MotifStats s = e.getValue();
                double pct = totalTrips == 0 ? 0.0 : 100.0 * s.count / totalTrips;

                dataset.addValue(pct, "Distribution (%)", e.getKey());
                dataset.addValue(s.avgMinutes(), "Durée moyenne (mn)", e.getKey());
            }

            var chart = ChartFactory.createBarChart(
                    "Déplacements par motif",
                    "Motif",
                    "Valeur",
                    dataset
            );

            ChartUtils.saveChartAsPNG(new File(file), chart, 1200, 700);
        }
    }
    
    static class DebugEventCounter implements BasicEventHandler {

        private final Map<String, Integer> counts = new TreeMap<>();

        @Override
        public void handleEvent(Event event) {
            counts.merge(event.getEventType(), 1, Integer::sum);
        }

        @Override
        public void reset(int iteration) {
            counts.clear();
        }

        public void printStats() {
            System.out.println("Event types:");
            counts.forEach((type, count) ->
                    System.out.println(type + " : " + count));
        }
    }
    
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		//String eventsFile = "/home/serigne-gueye/RECHERCHE/MATSIM/SPOT-SENEGAL/scenarios/cetud/output_cetud/ITERS/it.1/1.events.xml.gz";
		//String eventsFile = "/home/serigne-gueye/RECHERCHE/MATSIM/SPOT/scenarios/netmob2025/output_18_09_2025/output_events.xml.gz";
		String eventsFile = "/home/serigne-gueye/RECHERCHE/MATSIM/SPOT-SENEGAL/scenarios/cetud/output_cetud/output_events.xml.gz";
		
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        EventsManager events = EventsUtils.createEventsManager();

        ActivityStatsCollector actStats = new ActivityStatsCollector();
        TripStatsCollector tripStats = new TripStatsCollector();
        VehicleHistogramCollector vehHist = new VehicleHistogramCollector(900); // bin size 900s = 15 min

        events.addHandler(actStats);
        events.addHandler(tripStats);
        events.addHandler(vehHist);

        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(eventsFile);

        actStats.printStats();
        tripStats.printStats();
        //vehHist.printHistogram();
        /*
        actStats.plot("activity-stats.png");
        tripStats.plot("trip-stats.png");
        vehHist.plot("vehicle-histogram.png");
        	*/
        actStats.plot("activity-stats_cetud.png");
        tripStats.plot("trip-stats_cetud.png");
        vehHist.plot("vehicle-histogram_cetud.png");
        
        
        TripByMotifCollector motifStats = new TripByMotifCollector();
        DebugEventCounter debug = new DebugEventCounter();

        events.addHandler(actStats);
        events.addHandler(tripStats);
        events.addHandler(vehHist);
        events.addHandler(motifStats);
        events.addHandler(debug);

        reader.readFile(eventsFile);

        debug.printStats();
        motifStats.printStats();
        motifStats.writeCsv("distribution_deplacements_par_motif.csv");
        motifStats.plot("distribution_deplacements_par_motif.png");
	}

}
