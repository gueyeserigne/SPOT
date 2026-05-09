package cetud;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.nio.file.Path;

public class CsvTable {

    private final List<String> colonnes;
    private final List<Map<String, String>> lignes;

    public CsvTable(List<String> colonnes, List<Map<String, String>> lignes) {
        this.colonnes = colonnes;
        this.lignes = lignes;
    }

    public List<String> getColonnes() {
        return colonnes;
    }

    public List<Map<String, String>> getLignes() {
        return lignes;
    }

    public String getValeur(int numeroLigne, String nomColonne) {
        return lignes.get(numeroLigne).get(nomColonne);
    }
    
    public List<String> getLigne(int numeroLigne) {
    	
    	List<String> lig = new ArrayList<>();
    	
    	 for (int j = 0; j < colonnes.size(); j++) {
             String nomColonne = colonnes.get(j);
             lig.add(lignes.get(numeroLigne).get(nomColonne));
         }
        return lig;
    }

    public List<String> getColonne(String nomColonne) {
    	
    	List<String> col= new ArrayList<>();
    	
    	 for (int numeroLigne = 0; numeroLigne < lignes.size(); numeroLigne++) {
             col.add(lignes.get(numeroLigne).get(nomColonne));
         }
        return col;
    }

    
    public int nombreDeLignes() {
        return lignes.size();
    }

    public int nombreDeColonnes() {
        return colonnes.size();
    }
    
    public void Filtrer(Set<String> filtre, String nomColonne) {
    	int i = 0;
    	
    	while(i < nombreDeLignes()) {
    		if(! filtre.contains(lignes.get(i).get(nomColonne)))
    			lignes.remove(i);
    		else
    			i++;    		
    	}
    }

    public static CsvTable charger(Path cheminFichier) throws IOException {
    	System.out.println("cheminfichier = " + cheminFichier);
        List<String> lignesFichier = Files.readAllLines(cheminFichier, StandardCharsets.UTF_8);

        if (lignesFichier.isEmpty()) {
            throw new IOException("Le fichier CSV est vide.");
        }

        System.out.println(lignesFichier.get(0));
        List<String> colonnes = parserLigneCsv(lignesFichier.get(0));
        System.out.println("Colonnes Size = " + colonnes.size());
        System.out.println("Coloones = " + colonnes);
        List<Map<String, String>> lignes = new ArrayList<>();

        for (int i = 1; i < lignesFichier.size(); i++) {
            List<String> valeurs = parserLigneCsv(lignesFichier.get(i));

            Map<String, String> ligne = new LinkedHashMap<>();

            for (int j = 0; j < colonnes.size(); j++) {
                String valeur = j < valeurs.size() ? valeurs.get(j) : "";
                ligne.put(colonnes.get(j), valeur);
            }
            lignes.add(ligne);
        }

        return new CsvTable(colonnes, lignes);
    }

    private static List<String> parserLigneCsv(String ligne) {    	
    	List<String> valeurs = new ArrayList<>();
        StringBuilder valeurCourante = new StringBuilder();

        boolean entreGuillemets = false;

        for (int i = 0; i < ligne.length(); i++) {
            char caractere = ligne.charAt(i);

            if (caractere == '"') {
                if (entreGuillemets 
                        && i + 1 < ligne.length() 
                        && ligne.charAt(i + 1) == '"') {
                    valeurCourante.append('"');
                    i++;
                } else {
                    entreGuillemets = !entreGuillemets;
                }
            } else if (caractere == ';' && !entreGuillemets) {
                valeurs.add(valeurCourante.toString());
                valeurCourante.setLength(0);
            } else {
                valeurCourante.append(caractere);
            }
        }

        valeurs.add(valeurCourante.toString());
        return valeurs;
    }
    
     void afficher() {

    	System.out.println("Colonnes : " + nombreDeColonnes());
        System.out.println("Lignes : " + nombreDeLignes());
        
        for (int i = 0; i < nombreDeLignes(); i++) {
        	System.out.println(getLigne(i));
        }
        
    }
    
    public static void main(String[] args) throws Exception {    	
    		
	        CsvTable table = CsvTable.charger(Path.of("/home/serigne-gueye/RECHERCHE/MATSIM/SPOT-SENEGAL/Données/Individus.csv"));

	    	Set<String> joursimu = new HashSet<String>() {{
			    add("Lundi");
			}};

	        table.Filtrer(joursimu,"JourIndi");

	}
	
}