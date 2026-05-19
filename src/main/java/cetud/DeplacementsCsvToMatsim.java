package cetud;

//package votre.package;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;

import cetud.Quartiers.QuartierCetud;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class DeplacementsCsvToMatsim {

 private final Quartiers quartiers;
 private static String individuscsv;

 private final Map<String, String> modes = new HashMap<>();
 private final Map<String, FacilityRef> homeFacilityByPerson = new HashMap<>();
 private final Map<String, FacilityRef> workFacilityByPerson = new HashMap<>();
 private final Map<String, FacilityRef> otherFacilities = new HashMap<>();

 private Scenario scenario;
 private ActivityFacilities facilities;
 
 public void afficher() {
 	
 	for (Map.Entry<String, String> entry : modes.entrySet()) {
 	    String cle = entry.getKey();
 	    String valeur = entry.getValue();

 	    System.out.println("Clé : " + cle + " - " + valeur);
 	}
 }


 public DeplacementsCsvToMatsim(Quartiers quartiers,String individuscsv) {
     this.quartiers = quartiers;
     this.individuscsv = individuscsv;
 }

 public void run(Path deplacementsCsv,Path modesCetudCsv,Path plansOut,Path facilitiesOut,Set<String> joursimu) throws Exception {

     this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
     this.facilities = scenario.getActivityFacilities();

     readModes(modesCetudCsv);
     afficher();

     Map<String, List<Row>> rowsByPerson = readDeplacements(deplacementsCsv,joursimu);
     Population population = scenario.getPopulation();
     PopulationFactory pf = population.getFactory();

     for (Map.Entry<String, List<Row>> entry : rowsByPerson.entrySet()) {
         String personId = entry.getKey();
         System.out.println("personId = "+ personId);
         List<Row> rows = entry.getValue();
         rows.sort(Comparator.comparingInt(r -> r.numDepl));

         Person person = pf.createPerson(Id.createPersonId(personId));
         Plan plan = pf.createPlan();

         // Activité initiale : domicile au lieu de départ du premier déplacement.
         Row first = rows.get(0);
         FacilityRef current = getHomeFacility(personId, first.lieuDepart);
         Activity firstAct = pf.createActivityFromCoord("home", current.coord);
         firstAct.setFacilityId(current.id);
         firstAct.setEndTime(parseTimeToSeconds(first.heureDepart));
         plan.addActivity(firstAct);
         int i = 1;
         int n = rows.size();
         //for (Row r : rows) {
         while(i <= n) {
        	 Row r = rows.get(i-1);
        	 Row r_cur;
        	 if(i<n)
        		 r_cur = rows.get(i);
        	 else
        		 r_cur = rows.get(n-1);
        		 
             //Leg leg = pf.createLeg(mapMode(r.mode1));
        	 String mode = createMode(r);
        	 //Leg leg = pf.createLeg(mapMode(r.mode1));
        	 Leg leg = pf.createLeg(mapMode(mode));
             plan.addLeg(leg);

             String actType = mapMotifToActivity(r.motif);
             FacilityRef dest = getFacility(personId, actType, r.lieuArrivee, r.numDepl);

             Activity act = pf.createActivityFromCoord(actType, dest.coord);
             act.setFacilityId(dest.id);
             
             if(i < n)
            	 act.setEndTime(parseTimeToSeconds(r_cur.heureDepart) );
             else
            	 act.setStartTime(parseTimeToSeconds(r_cur.heureArrivee) );
             /*
             if (r.numDepl != rows.get(rows.size() - 1).numDepl) {
                 act.setEndTime(parseTimeToSeconds(r.heureArrivee) + parseDurationToSeconds(r.duree));
             }*/

             plan.addActivity(act);
             i++;
         }

         person.addPlan(plan);
         population.addPerson(person);
     }

     new PopulationWriter(population).write(plansOut.toString());
     new FacilitiesWriter(facilities).write(facilitiesOut.toString());
     afficher();
 }

 private FacilityRef getFacility(String personId, String actType, String lieu, int numDepl) {
     if ("home".equals(actType)) {
         return getHomeFacility(personId, lieu);
     }
     if ("work".equals(actType)) {
         return getWorkFacility(personId, lieu);
     }
     
     //return getHomeFacility(personId, lieu);

     String key = personId + "|" + actType + "|" + numDepl;
     return otherFacilities.computeIfAbsent(key, k ->
             createFacility("fac_" + personId + "_" + actType + "_" + numDepl, actType, lieu));
             
 }

 private FacilityRef getHomeFacility(String personId, String lieu) {
     return homeFacilityByPerson.computeIfAbsent(personId, k ->
             createFacility("home_" + personId, "home", lieu));
 }

 private FacilityRef getWorkFacility(String personId, String lieu) {
     return workFacilityByPerson.computeIfAbsent(personId, k ->
             createFacility("work_" + personId, "work", lieu));
 }

 private FacilityRef createFacility(String idStr, String actType, String lieu) {
	 
	 Map<String, QuartierCetud> quartiers_cetud = quartiers.getQuartiersCetud();
	 
	 //System.out.println("Lieu = "+ lieu);
	 String strate = quartiers_cetud.get(lieu).strate;
	 //System.out.println("Strate = "+ strate);
     Quartiers.PointGeo p = quartiers.getPointAleatoireDansLimiteAdministrative(strate);
     CoordinateTransformation ct =
    		    TransformationFactory.getCoordinateTransformation(
    		        TransformationFactory.WGS84,
    		        "EPSG:32628" // UTM zone 28N (Sénégal)
    		    );

     Coord coord = ct.transform(new Coord(p.lon, p.lat));
   

     Id<ActivityFacility> id = Id.create(idStr, ActivityFacility.class);
     ActivityFacility facility = facilities.getFactory().createActivityFacility(id, coord);

     ((ActivityFacilityImpl) facility).createAndAddActivityOption(actType);
     facilities.addActivityFacility(facility);

     return new FacilityRef(id, coord);
 }

 private static String mapMotifToActivity(String motif) {
     String m = clean(motif);

     if (m.equals("1") || m.equalsIgnoreCase("TravailHabituel") || m.equalsIgnoreCase("Travail habituel")) {
         return "work";
     }
     if (m.equals("23") || m.equalsIgnoreCase("RetourDomicile") || m.equalsIgnoreCase("Retour domicile")) {
         return "home";
     }

     return switch (m) {
         case "2", "AutreMotifTravail" -> "other_work";
         case "3", "TravailAmbulant" -> "mobile_work";
         case "4", "RechercheTravail" -> "job_search";
         case "5", "Etudes" -> "education";
         case "6", "AutreMotifEtudes" -> "other_education";
         case "7", "AchatsAlimentaires" -> "food_shopping";
         case "8", "AchatsNonAlimentaires" -> "shopping";
         case "9", "ApprovisionnementEau" -> "water";
         case "10", "DemarchesAdministratives" -> "administrative";
         case "11", "Services" -> "services";
         case "12", "Sante" -> "health";
         case "13", "AutreMotifMenage" -> "household";
         case "14", "RepasExterieur" -> "restaurant";
         case "15", "VisiteFamille" -> "family_visit";
         case "16", "VisiteAmis" -> "friends_visit";
         case "17", "VisitesVoisins" -> "neighbour_visit";
         case "18", "Religion" -> "religion";
         case "19", "Ceremonies" -> "ceremony";
         case "20", "Associations" -> "association";
         case "21", "SportsLoisirs" -> "leisure";
         case "22", "Accompagnement" -> "escort";
         case "24", "Autre" -> "other";
         default -> "other";
     };
 }
 
 
 private String mapMode(String modeCode) {
	    String m = clean(modeCode)
	            .replace("\"", "")
	            .replace(" ", "")
	            .toLowerCase();

	    return switch (m) {
	        case "apied", "a_pied", "marche", "walk" -> "walk";
	        case "bicyclette", "velo", "vélo", "bike" -> "bike";
	        case "voiture", "voitureconducteur", "voitureparticuliere", "car" -> "car";
	        case "voitureparticuliereconducteur", "voitureparticulierepassager" -> "car";
	        case "taxi", "taxiclando", "mobylettemotopassager", "mobylettemotoconducteur" -> "car";
	        case "bus", "minibus", "car_rapide", "transportcollectif", "carrapide", "dakardemdik", "tata", "pt" -> "pt";
	        default -> "walk"; // par sécurité
	    };
	}
 
 


 private String createMode(Row r) {

	    if (Integer.parseInt(r.nbtrajets) == 1) {
	        return mapMode(r.mode1);
	    }

	    System.out.println(r.mode1 + " " + r.mode2 + " " + r.mode3 + " " + r.mode4);

	    String selectedMode = firstMotorizedOrPtMode(r.mode1, r.mode2, r.mode3, r.mode4);

	    if (!selectedMode.isBlank()) {
	        return mapMode(selectedMode);
	    }

	    return mapMode(r.mode1);
	}

	private String firstMotorizedOrPtMode(String... modeCodes) {
	    for (String modeCode : modeCodes) {
	        String code = clean(modeCode);

	        if (modes.containsKey(code)) {
	            String type = clean(modes.get(code));

	            if (type.equals("motorise") || type.equals("transportcommun")) {
	                return code;
	            }
	        }
	    }

	    return "";
	}
 private void readModes(Path file) throws IOException {
     try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
         String header = br.readLine();
         String line;
         while ((line = br.readLine()) != null) {
             String[] c = splitCsv(line);
             if (c.length >= 3) {
                 //modes.put(clean(c[0]), normalizeMode(c[2]));
            	// modes.put(normalizeMode(c[2]),normalizeMode(c[3]));
            	 modes.put(clean(c[2]),normalizeMode(c[3]));
             }
         }
     }
 }

 private static String normalizeMode(String raw) {
     String m = clean(raw);
     if (m.equalsIgnoreCase("APied")) return "walk";
     if (m.equalsIgnoreCase("Bicyclette")) return "bike";
     if (m.toLowerCase().contains("voiture")) return "car";
     if (m.toLowerCase().contains("mobylette")) return "car";
     if (m.toLowerCase().contains("taxi")) return "taxi";
     if (m.toLowerCase().contains("bus")) return "pt";
     if (m.toLowerCase().contains("car")) return "pt";
     if (m.equalsIgnoreCase("CarRapide")) return "pt";
     if (m.equalsIgnoreCase("NdiagaNdiaye")) return "pt";
     if (m.equalsIgnoreCase("Tata")) return "pt";
     if (m.equalsIgnoreCase("DakarDemDik")) return "pt";
     return m;
 }


 
 
 private static Map<String, List<Row>> readDeplacements(Path file, Set<String> joursimu) throws Exception {
     Map<String, List<Row>> byPerson = new LinkedHashMap<>();
     Set<String> individus = new HashSet<String>(); // Ensemble des individus concernés par les jours de simulation dans joursimu
     //String individuscsv = "/home/serigne-gueye/RECHERCHE/MATSIM/SPOT/Données/Individus_1.csv";
          
     PlanCetud plancetud = new PlanCetud(individuscsv);

     Map<String, Set<String>> jours_individus = plancetud.getJoursIndividus();
     
     for(String jour : joursimu)
    	 individus.addAll(jours_individus.get(jour));     
    		 
     try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
         String[] header = splitCsv(br.readLine());
         /*
         System.out.print("header : ");
         for(int i = 0; i < header.length; i++)
        	 System.out.print(header[i] + " ");
         */
         Map<String, Integer> idx = index(header);

         String line;
         while ((line = br.readLine()) != null) {
             String[] c = splitCsv(line);
             String personId = value(c, idx, "numMena")+"_"+value(c, idx, "numIndi");
	         if(individus.isEmpty() || individus.contains(personId)) {
	             Row r = new Row();
	             r.numMena = value(c, idx, "numMena");
	             r.numIndi = value(c, idx, "numIndi");
	             //System.out.println("line = " + line);
	             //System.out.println("value = " + value(c, idx, "numDepl"));
	             r.numDepl = Integer.parseInt(value(c, idx, "numDepl"));
	             r.lieuDepart = value(c, idx, "LieuDepart");
	             r.lieuArrivee = valueAny(c, idx, "LieuArrivee", "LieuArrivée");
	             r.heureDepart = value(c, idx, "HeureDepart");
	             r.heureArrivee = valueAny(c, idx, "HeureArrivee", "HeureArrivée");
	             r.nbtrajets = value(c, idx, "NbTrajets");
	             r.duree = value(c, idx, "Duree");
	             r.motif = value(c, idx, "Motif");
	             r.mode1 = value(c, idx, "Mode1");
	             r.mode2 = value(c, idx, "Mode2");
	             r.mode3 = value(c, idx, "Mode3");
	             r.mode4 = value(c, idx, "Mode4");
	
	             byPerson.computeIfAbsent(r.numMena+"_"+r.numIndi, k -> new ArrayList<>()).add(r);
	         }
         }
     }

     return byPerson;
 }

 private static Map<String, Integer> index(String[] header) {
     Map<String, Integer> idx = new HashMap<>();
     for (int i = 0; i < header.length; i++) {
         idx.put(clean(header[i]), i);
     }
     return idx;
 }

 private static String value(String[] row, Map<String, Integer> idx, String name) {
     Integer i = idx.get(clean(name));
     //System.out.println("i = " + i);
     //System.out.println("name = " + name);
     return i == null || i >= row.length ? "" : clean(row[i]);
 }

 private static String valueAny(String[] row, Map<String, Integer> idx, String... names) {
     for (String name : names) {
         String v = value(row, idx, name);
         if (!v.isBlank()) return v;
     }
     return "";
 }

 private static String[] splitCsv(String line) {
     return line.split(";", -1);
 }

 private static String clean(String s) {
     return s == null ? "" : s.trim().replace("\uFEFF", "").replace("\"", "");
 }

 private static double parseDurationToSeconds(String minutes) {
     if (clean(minutes).isBlank()) return 0;
     return Double.parseDouble(clean(minutes).replace(",", ".")) * 60.0;
 }

 private static double parseTimeToSeconds(String value) {
     String v = clean(value);
     if (v.isBlank()) return 0;

     if (v.contains(":")) {
         String[] p = v.split(":");
         int h = Integer.parseInt(p[0]);
         int m = p.length > 1 ? Integer.parseInt(p[1]) : 0;
         int s = p.length > 2 ? Integer.parseInt(p[2]) : 0;
         return h * 3600.0 + m * 60.0 + s;
     }

     double numeric = Double.parseDouble(v.replace(",", "."));

     // Cas Excel / ODS : fraction de journée.
     if (numeric > 0 && numeric < 1) {
         return numeric * 24.0 * 3600.0;
     }

     // Cas HHMM, ex. 730 = 07:30.
     if (numeric >= 100 && numeric <= 2359) {
         int hhmm = (int) numeric;
         int h = hhmm / 100;
         int m = hhmm % 100;
         return h * 3600.0 + m * 60.0;
     }

     // Cas heure décimale.
     return numeric * 3600.0;
 }

 private record FacilityRef(Id<ActivityFacility> id, Coord coord) {}

 private static class Row {
	 String numMena;
	 String numIndi;
     int numDepl;
     String lieuDepart;
     String lieuArrivee;
     String heureDepart;
     String heureArrivee;
     String nbtrajets;
     String duree;
     String motif;
     String mode1;
     String mode2;
     String mode3;
     String mode4;
 }
/*
 public interface Quartiers {
     PointGeo getPointAleatoireDansLimiteAdministrative(String codeQuartier);
 }
*/
 
 /*
 public interface PointGeo {
     double getX();
     double getY();
 }
*/
 public static void main(String[] args) throws Exception {
     /*
      * Exemple :
      * Quartiers quartiers = new VotreClasseQuartiers(Paths.get("codes_quartiers_strates_cetud.csv"));
      */
     //Quartiers quartiers = null;
	 
	 //String dir = "/home/serigne-gueye/RECHERCHE/MATSIM/SPOT/scenarios/cetud/";
	 String dir = args[0];
	 Set<String> joursimu;
	 
	 if(args[1].equals("joursouvres")) {
		 joursimu = new HashSet<String>() {{
		    add("Mardi");
		    add("Mercredi");
		    add("Jeudi");
		    add("Vendredi");
		    add("Samedi");
		}};
	 }
	 else {
		 joursimu = new HashSet<String>() {{
			    add("Dimanche");
			}};
	 }
	 
	 String individuscsv = args[2];
	 
     Quartiers quartiers = new Quartiers(
             dir+"quartiers_centroids.csv",
             dir+"dakar-guediawaye-pikine-thies.geojson"
     );

     quartiers.lireCsvCetud(dir+"codes_quartiers_strates_cetud.csv");
     
     new DeplacementsCsvToMatsim(quartiers,individuscsv).run(
             Paths.get(dir+"Deplacements.csv"),
             Paths.get(dir+"modes_cetud.csv"),
             Paths.get(dir+"plans_samedi.xml"),
             Paths.get(dir+"facilities_samedi.xml"),
             joursimu);
 }
}