package cetud;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class PlanCetud {
	// Map qui fait correspondre chaque jour à l'ensemble des individus enquêtés ce jour
	private final Map<String, Set<String>> jours_individus = new HashMap<>();
	private final CsvTable tableIndividus;
	private final CsvTable tableDeplacements;
	
	
	public final Map<String, Set<String>> getJoursIndividus(){
		return(jours_individus);
	}
	
    public PlanCetud(String csvIndividus, String csvDeplacements) throws Exception {
    	 tableIndividus = CsvTable.charger(Path.of(csvIndividus));
    	 tableDeplacements = CsvTable.charger(Path.of(csvDeplacements));

    	 for (int i = 0; i < tableIndividus.nombreDeLignes(); i++) {
    		 String JourIndi = tableIndividus.getValeur(i, "JourIndi");
    		 String numIndi = tableIndividus.getValeur(i, "numIndi");
    		 String numMena = tableIndividus.getValeur(i, "numMena");
    		 // L'identifiant d'un individu est : numMena+"_"+numIndi
    		 if(jours_individus.containsKey(JourIndi))
    			 jours_individus.get(JourIndi).add(numMena+"_"+numIndi);
    		 else {
    			 Set<String> individus = new HashSet<String>() {{add(numMena+"_"+numIndi);}};
    			 jours_individus.put(JourIndi, individus);
    		 }
    	}
    			 
	}
	
    public PlanCetud(String csvIndividus) throws Exception {
   	 tableIndividus = CsvTable.charger(Path.of(csvIndividus));
	 this.tableDeplacements = null;

   	 for (int i = 0; i < tableIndividus.nombreDeLignes(); i++) {
   		 String JourIndi = tableIndividus.getValeur(i, "JourIndi");
   		 String numIndi = tableIndividus.getValeur(i, "numIndi");
   		 String numMena = tableIndividus.getValeur(i, "numMena");
   		 // L'identifiant d'un individu est : numMena+"_"+numIndi
   		 if(jours_individus.containsKey(JourIndi))
   			 jours_individus.get(JourIndi).add(numMena+"_"+numIndi);
   		 else {
   			 Set<String> individus = new HashSet<String>() {{add(numMena+"_"+numIndi);}};
   			 jours_individus.put(JourIndi, individus);
   		 }
   	}
   			 
	}

    public void filtrer(Set<String> filtre, String nomColonne) {
    	
    	Set<String> individus = new HashSet<String>();
    	
    	for (String element : filtre) {
    		individus.addAll(jours_individus.get(element));
    	}	
    	
    	System.out.print("Individus à conserver = " );
    	for (String element : individus) {
    		System.out.println(" " + element);
    	}
    	
    	//tableDeplacements.Filtrer(individus, nomColonne);
    }

    public void afficher() {
    	//tableDeplacements.afficher();
    	
    	for (Map.Entry<String, Set<String>> entry : jours_individus.entrySet()) {
    	    String cle = entry.getKey();
    	    Set<String> valeurs = entry.getValue();

    	    System.out.println("Clé : " + cle);

    	    for (String val : valeurs) {
    	        System.out.println("  - " + val);
    	    }
    	}
    }

	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		
		 String deplacementscsv = "/home/serigne-gueye/RECHERCHE/MATSIM/SPOT-SENEGAL/Données/Deplacements.csv";
		 String individuscsv = "/home/serigne-gueye/RECHERCHE/MATSIM/SPOT-SENEGAL/Données/Individus_1.csv";
		 //CsvTable table = CsvTable.charger(Path.of("/home/serigne-gueye/RECHERCHE/MATSIM/SPOT-SENEGAL/Données/Deplacements.csv"));
		 
		 PlanCetud plancetud = new PlanCetud(individuscsv,deplacementscsv);
		 
		 plancetud.afficher();
		 
		 Set<String> joursimu = new HashSet<String>() {{
			    add("Jeudi");
			}};

	       plancetud.filtrer(joursimu,"numIndi");

	}

}
