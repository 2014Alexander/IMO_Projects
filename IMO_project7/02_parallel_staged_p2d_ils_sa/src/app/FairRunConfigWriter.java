package app;

import faircomparison.FairRunConfig;
import faircomparison.FairRunConfigCsv;
import faircomparison.FairRunPlan;
import io.CsvInstanceReader;
import model.Instance;

import java.nio.file.Path;
import java.util.List;

/** Writes fixed all-vertices run configs without executing tested algorithms. */
public final class FairRunConfigWriter {
    private static final long DEFAULT_BASE_SEED = 20260614L;
    private static final String DEFAULT_CONFIG_DIR = "configs";

    public static void main(String[] args) throws Exception {
        long baseSeed = args.length >= 1 ? Long.parseLong(args[0]) : DEFAULT_BASE_SEED;
        String configDir = args.length >= 2 ? args[1] : DEFAULT_CONFIG_DIR;
        String[] instancePaths = args.length >= 3 ? instancePaths(args, 2) : new String[] {"data/TSPA.csv", "data/TSPB.csv"};

        for (String instancePath : instancePaths) {
            Instance instance = new CsvInstanceReader().read(instancePath);
            List<FairRunConfig> configs = FairRunPlan.allVertices(instance, baseSeed);
            Path out = Path.of(configDir, instance.name + "_run_configs_" + configs.size() + ".csv");
            FairRunConfigCsv.write(out, configs);
            System.out.println("wrote " + out + " rows=" + configs.size());
        }
    }

    private static String[] instancePaths(String[] args, int offset) {
        String[] paths = new String[args.length - offset];
        System.arraycopy(args, offset, paths, 0, paths.length);
        return paths;
    }
}
