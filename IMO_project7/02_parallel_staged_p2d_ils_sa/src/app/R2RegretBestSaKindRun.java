package app;

import algorithm.IterationCountingAlgorithm;
import algorithm.OptimizationAlgorithm;
import algorithm.construction.K8M3StdWithPhaseTwoDelete;
import algorithm.construction.Randomized2RegretBestNWithPhaseTwoDeleteStart;
import algorithm.localsearch.SteepestLocalSearchWithCandidateMoves;
import algorithm.metaheuristic.CoolingSchedule;
import algorithm.metaheuristic.IteratedLocalSearchWithConstructionStartAndSaAcceptance;
import algorithm.metaheuristic.IteratedLocalSearchWithTwoRegretStartAndSaAcceptance;
import algorithm.metaheuristic.SaAcceptanceStatistics;
import algorithm.metaheuristic.perturbation.RandomSwapEdgesPerturbation;
import evaluation.SolutionEvaluator;
import experiment.core.RunConfig;
import experiment.core.RunPreparator;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;
import java.util.List;

public final class R2RegretBestSaKindRun {
    private static final long TIME_LIMIT_NANOS = 500_000_000L;
    private static final double T0 = 300.0;
    private static final double TMIN = 10.0;
    private static final CoolingSchedule COOLING = CoolingSchedule.GEOMETRIC;
    public static void main(String[] args) {
        String kind=args[0]; String path=args[1]; int runsCount=Integer.parseInt(args[2]); long baseSeed=Long.parseLong(args[3]);
        Instance instance=new CsvInstanceReader().read(path); List<RunConfig> runs=RunPreparator.prepareRuns(instance,baseSeed,runsCount);
        System.out.println("instance,algorithm,runIndex,startVertexId,seed,objective,runtimeNanos,iterationCount,acceptedBetterCount,acceptedWorseCount,rejectedWorseCount,bestFoundIteration,cooling,T0,Tmin");
        for(int i=0;i<runs.size();i++){
            RunConfig run=runs.get(i); OptimizationAlgorithm alg=create(kind,run.runSeed());
            long st=System.nanoTime(); Solution sol=alg.solve(instance,run.startVertexId()); long rt=System.nanoTime()-st;
            int obj=new SolutionEvaluator().evaluate(instance,sol).objective(); int it=((IterationCountingAlgorithm)alg).lastIterationCount(); SaAcceptanceStatistics stats=(SaAcceptanceStatistics)alg;
            System.out.println(instance.name+","+alg.name()+","+i+","+run.startVertexId()+","+run.runSeed()+","+obj+","+rt+","+it+","+stats.acceptedBetterCount()+","+stats.acceptedWorseCount()+","+stats.rejectedWorseCount()+","+stats.bestFoundIteration()+","+COOLING+","+T0+","+TMIN); System.out.flush();
        }
    }
    private static OptimizationAlgorithm create(String kind,long seed){
        if(kind.equals("k8")) return construction("ILS_K8_M3_STD_P2D_START_SA_GEO_T0_300_TMIN_10",new K8M3StdWithPhaseTwoDelete(),seed);
        if(kind.equals("r2top3best5")) return r2("ILS_R2REGRET_TOP3P20_BEST5_P2D_LS_START_SA_GEO_T0_300_TMIN_10",false,5,seed);
        if(kind.equals("r2top3best10")) return r2("ILS_R2REGRET_TOP3P20_BEST10_P2D_LS_START_SA_GEO_T0_300_TMIN_10",false,10,seed);
        if(kind.equals("r2top5best5")) return r2("ILS_R2REGRET_TOP5P20_BEST5_P2D_LS_START_SA_GEO_T0_300_TMIN_10",true,5,seed);
        if(kind.equals("r2top5best10")) return r2("ILS_R2REGRET_TOP5P20_BEST10_P2D_LS_START_SA_GEO_T0_300_TMIN_10",true,10,seed);
        return new IteratedLocalSearchWithTwoRegretStartAndSaAcceptance("ILS_2REGRET_P2D_START_SA_GEO_T0_300_TMIN_10",new SteepestLocalSearchWithCandidateMoves(),new RandomSwapEdgesPerturbation(30),TIME_LIMIT_NANOS,seed,T0,TMIN,COOLING);
    }
    private static OptimizationAlgorithm r2(String name,boolean top5,int tries,long seed){return construction(name,new Randomized2RegretBestNWithPhaseTwoDeleteStart(name+"_START",top5,tries,seed,new SteepestLocalSearchWithCandidateMoves()),seed);}
    private static OptimizationAlgorithm construction(String name,OptimizationAlgorithm start,long seed){return new IteratedLocalSearchWithConstructionStartAndSaAcceptance(name,start,new SteepestLocalSearchWithCandidateMoves(),new RandomSwapEdgesPerturbation(30),TIME_LIMIT_NANOS,seed,T0,TMIN,COOLING);}
}
