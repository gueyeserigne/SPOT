package cetud;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

public class Network {

    public static void main(String[] args) {
    	
    	String dir = "/home/serigne-gueye/RECHERCHE/MATSIM/SPOT-SENEGAL/scenarios/cetud/";

        String inputOsm = dir+"dakar-guediawaye-pikine-thies-roads.osm";
        String outputNetwork = dir+"network.xml.gz";

        createNetwork(inputOsm, outputNetwork);
    }

    public static void createNetwork(String inputOsm, String outputNetwork) {

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        CoordinateTransformation transformation =
                TransformationFactory.getCoordinateTransformation(
                        TransformationFactory.WGS84,
                        "EPSG:32628"
                );

        OsmNetworkReader reader =
                new OsmNetworkReader(
                        scenario.getNetwork(),
                        transformation
                );

        reader.parse(inputOsm);

        new NetworkCleaner().run(scenario.getNetwork());

        new NetworkWriter(scenario.getNetwork()).write(outputNetwork);

        System.out.println("Network créé : " + outputNetwork);
        System.out.println("Nodes : " + scenario.getNetwork().getNodes().size());
        System.out.println("Links : " + scenario.getNetwork().getLinks().size());
    }
}