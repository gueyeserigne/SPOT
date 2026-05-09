package cetud;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class Simulation {

    public static void main(String[] args) {

        //String dir = "/home/serigne-gueye/RECHERCHE/MATSIM/SPOT-SENEGAL/scenarios/cetud/";
    	String dir = args[0];

        String configFile = dir + "config.xml";
        String networkFile = dir + "network.xml.gz";
        String plansFile = dir + "plans.xml";
        String facilitiesFile = dir + "facilities.xml";

        run(configFile, networkFile, plansFile, facilitiesFile);
    }

    public static void run(
            String configFile,
            String networkFile,
            String plansFile,
            String facilitiesFile
    ) {

        Config config = ConfigUtils.loadConfig(configFile);

        config.network().setInputFile(networkFile);
        config.plans().setInputFile(plansFile);
        config.facilities().setInputFile(facilitiesFile);

        // Dossier de sortie MATSim
        //config.controller().setOutputDirectory("output-cetud");

        // Écrase le dossier de sortie si déjà existant
        config.controller().setOverwriteFileSetting(
                org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists
        );

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);

        controler.run();

        System.out.println("Simulation MATSim terminée.");
    }
}