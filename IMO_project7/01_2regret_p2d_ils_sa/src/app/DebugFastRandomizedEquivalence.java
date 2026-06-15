package app;

import algorithm.SolutionObjective;
import algorithm.construction.FastRandomized2RegretParameterizedWithPhaseTwoDelete;
import algorithm.construction.Randomized2RegretParameterizedWithPhaseTwoDelete;
import io.CsvInstanceReader;
import model.Instance;
import model.Solution;

import java.util.HashSet;
import java.util.Random;

public final class DebugFastRandomizedEquivalence {
    public static void main(String[] args) {
        Instance instance = new CsvInstanceReader().read(args[0]);
        int start = Integer.parseInt(args[1]);
        long seed = Long.parseLong(args[2]);
        Solution s1 = new Randomized2RegretParameterizedWithPhaseTwoDelete("std",3,0.30,new Random(seed)).solve(instance,start);
        Solution s2 = new FastRandomized2RegretParameterizedWithPhaseTwoDelete("fast",3,0.30,new Random(seed)).solve(instance,start);
        System.out.println("std obj=" + SolutionObjective.calculate(instance,s1) + " size=" + s1.cycle().size() + " valid=" + valid(s1, instance.size));
        System.out.println("fast obj=" + SolutionObjective.calculate(instance,s2) + " size=" + s2.cycle().size() + " valid=" + valid(s2, instance.size));
        System.out.println("sameCycle=" + s1.cycle().equals(s2.cycle()));
        if(!s1.cycle().equals(s2.cycle())){
            for(int i=0;i<Math.min(s1.cycle().size(),s2.cycle().size());i++){
                if(!s1.cycle().get(i).equals(s2.cycle().get(i))){System.out.println("firstDiff="+i+" std="+s1.cycle().get(i)+" fast="+s2.cycle().get(i));break;}
            }
        }
    }
    private static boolean valid(Solution s, int n){
        HashSet<Integer> seen = new HashSet<>();
        for(int v:s.cycle()) if(v<0 || v>=n || !seen.add(v)) return false;
        return true;
    }
}
