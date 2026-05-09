package cetud;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Quartiers {

    public static class QuartierCetud {
        public String code;
        public String strate;
        public String nom;

        public QuartierCetud(String code,String nom,String strate) {
            this.code = code;
            this.nom = nom;
            this.strate = strate;
            
        }
    }

	
    public static class Quartier {
        public String strate;
        public String nom;
        public double lon;
        public double lat;
        public String adminLevel;
        public Geometry geometry;

        public Quartier(String strate, String nom, double lon, double lat, String adminLevel) {
            this.strate = strate;
            this.nom = nom;
            this.lon = lon;
            this.lat = lat;
            this.adminLevel = adminLevel;
        }
    }

    public static class PointGeo {
        public final double lon;
        public final double lat;

        public PointGeo(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
        }

        @Override
        public String toString() {
            return lon + "," + lat;
        }
    }

    private final Map<String, Quartier> quartiers = new HashMap<>();
    private final Map<String, QuartierCetud> quartiers_cetud = new HashMap<>();
    private final Map<String, String> name_strate = new HashMap<>();
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final Random random = new Random();
    
    public void afficher() {
    	
    	for (Map.Entry<String, String> entry : name_strate.entrySet()) {
    	    String cle = entry.getKey();
    	    String valeur = entry.getValue();

    	    System.out.println("Clé : " + cle + " - Valeurs : " + valeur);
    	}
    }


    public Quartiers(String csvFile, String geojsonFile) throws Exception {
        lireCsv(csvFile);
        lireGeoJson(geojsonFile);
    }

    private void lireCsv(String csvFile) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String header = br.readLine();
            String[] cols = header.split(",");

            int idxStrate = indexOf(cols, "Strate");
            int idxQuartier = indexOf(cols, "Quartier");
            int idxLon = indexOf(cols, "lon");
            int idxLat = indexOf(cols, "lat");
            int idxAdmin = indexOf(cols, "admin_level");
     

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] v = line.split(",");

                Quartier q = new Quartier(
                        v[idxStrate].trim(),
                        v[idxQuartier].trim(),
                        Double.parseDouble(v[idxLon].trim()),
                        Double.parseDouble(v[idxLat].trim()),
                        v[idxAdmin].trim()
                );

                quartiers.put(clean(q.strate), q);
                name_strate.put(clean(q.nom),clean(q.strate));
                //System.out.println("+code : " + q.strate);
                //System.out.println("+name : " + q.nom);
                //quartiers.put(q.nom, q);
            }
        }
        
        //afficher();
    }
    
    private static String clean(String s) {
        return s == null ? "" : s.trim().replace("\uFEFF", "").replace("\"", "");
    }


     void lireCsvCetud(String csvFile) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String header = br.readLine();
            String[] cols = header.split(";");

            int idxCode = indexOf(cols, "Code");
            int idxQuartier = indexOf(cols, "Quartier");
            int idxStrate = indexOf(cols, "Strate");

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] v = line.split(";");

                QuartierCetud q = new QuartierCetud(
                        v[idxCode].trim(),
                        v[idxQuartier].trim(),
                        v[idxStrate].trim()
                );

                quartiers_cetud.put(q.code, q);
                System.out.println(q.code + "," + q.nom + "," + q.strate);
            }
        }
    }

    
    private void lireGeoJson(String geojsonFile) throws Exception {
        String content = Files.readString(Path.of(geojsonFile));

        // Lecture simple d'un FeatureCollection GeoJSON
        System.out.println("****************************************");
        String[] features = content.split("\"type\"\\s*:\\s*\"Feature\"");

        GeoJsonReader reader = new GeoJsonReader(geometryFactory);
        //afficher();
        for (String feature : features) {
            if (!feature.contains("\"geometry\"")) continue;

            //String code = extractProperty(feature, "code");
            String name = extractProperty(feature, "name");
           //System.out.print("*** name : " + clean(name));
           String code = name_strate.get(clean(name));
           
           // System.out.println("*** code : " + code);
            if (name == null) continue;

            Quartier quartier = quartiers.get(clean(code));
            if (quartier == null) continue;

            String geometryJson = extractGeometryJson(feature);
            if (geometryJson == null) continue;

            Geometry geom = reader.read(geometryJson);

            if (geom instanceof Polygon || geom instanceof MultiPolygon) {
                quartier.geometry = geom;
            }
        }
    }

    private String extractProperty(String feature, String propertyName) {
        String pattern = "\"" + propertyName + "\"";
        int idx = feature.indexOf(pattern);
        if (idx < 0) return null;

        int colon = feature.indexOf(":", idx);
        int firstQuote = feature.indexOf("\"", colon + 1);
        int secondQuote = feature.indexOf("\"", firstQuote + 1);

        if (firstQuote < 0 || secondQuote < 0) return null;

        return feature.substring(firstQuote + 1, secondQuote);
    }

    private String extractGeometryJson(String feature) {
        int geomIdx = feature.indexOf("\"geometry\"");
        if (geomIdx < 0) return null;

        int start = feature.indexOf("{", geomIdx);
        if (start < 0) return null;

        int depth = 0;

        for (int i = start; i < feature.length(); i++) {
            char c = feature.charAt(i);

            if (c == '{') depth++;
            if (c == '}') depth--;

            if (depth == 0) {
                return feature.substring(start, i + 1);
            }
        }

        return null;
    }

    private int indexOf(String[] cols, String colName) {
        for (int i = 0; i < cols.length; i++) {
            if (cols[i].trim().equals(colName)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Colonne manquante : " + colName);
    }

    public PointGeo getCentroide(String codeQuartier) {
        Quartier q = quartiers.get(codeQuartier);

        if (q == null) {
            throw new IllegalArgumentException("Quartier inconnu : " + codeQuartier);
        }

        return new PointGeo(q.lon, q.lat);
    }

    public PointGeo getPointAleatoireDansLimiteAdministrative(String codeQuartier) {
        Quartier q = quartiers.get(codeQuartier);

        if (q == null) {
            throw new IllegalArgumentException("Quartier inconnu : " + codeQuartier);
        }

        if (q.geometry == null) {
            throw new IllegalArgumentException(
                    "Pas de polygone GeoJSON trouvé pour le quartier : " + codeQuartier
            );
        }
/*
        Envelope env = q.geometry.getEnvelopeInternal();

        for (int i = 0; i < 10000; i++) {
            double lon = env.getMinX() + random.nextDouble() * env.getWidth();
            double lat = env.getMinY() + random.nextDouble() * env.getHeight();

            Point point = geometryFactory.createPoint(new Coordinate(lon, lat));

            if (q.geometry.contains(point)) {
                return new PointGeo(lon, lat);
            }
        }
        */
        
        Envelope env = q.geometry.getEnvelopeInternal();

        double centerLon = env.centre().getX();
        double centerLat = env.centre().getY();

        // rayon en degrés (si coordonnées WGS84)
        double r = 0.01;
        
        for (int i = 0; i < 10000; i++) {
	        // distribution uniforme dans le disque
        	double angle = 2.0 * Math.PI * random.nextDouble();
	        double radius = r * Math.sqrt(random.nextDouble());
	
	        double lon = centerLon + radius * Math.cos(angle);
	        double lat = centerLat + radius * Math.sin(angle);
	        
	        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
	
	        if (q.geometry.contains(point)) {
	            return new PointGeo(lon, lat);
	        }
        }
        
        throw new RuntimeException(
                "Impossible de tirer un point dans le polygone après 10000 essais : " + codeQuartier
        );
    }

    public Map<String, Quartier> getQuartiers() {
        return quartiers;
    }

    public Map<String, QuartierCetud> getQuartiersCetud() {
        return quartiers_cetud;
    }

    
    public static void main(String[] args) throws Exception {
    	String dir = "/home/serigne-gueye/RECHERCHE/MATSIM/SPOT-SENEGAL/scenarios/cetud/";
    			
        Quartiers quartiers = new Quartiers(
                dir+"quartiers_centroids.csv",
                dir+"dakar-guediawaye-pikine-thies.geojson"
        );

        PointGeo p = quartiers.getPointAleatoireDansLimiteAdministrative("D00");

        System.out.println("Point aléatoire : " + p);
        
        quartiers.lireCsvCetud(dir+"codes_quartiers_strates_cetud.csv");
    }
}