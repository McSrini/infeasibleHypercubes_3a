/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a.utils;

import static ca.mcmaster.infeasiblehypercubes_3a.Constants.*;
import ca.mcmaster.infeasiblehypercubes_3a.collection.LeafNode;
import ca.mcmaster.infeasiblehypercubes_3a.collection.Rectangle;
import ca.mcmaster.infeasiblehypercubes_3a.common.VariableCoefficientTuple;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

/**
 *
 * @author tamvadss
 * 
 * considers best LP rectangles and finds most shared variable
 * 
 */
public class BranchingVariableSuggestor {
    
    //suggests the var with highest frequency that has not already been branched upon by this leaf
    public static VariableCoefficientTuple suggest (Map<Double, List<Rectangle>  > compatibleRectMap , LeafNode leaf){
        
         
        VariableCoefficientTuple varAndFrequency = new  VariableCoefficientTuple(null, ZERO);
        
        List<Rectangle> rectanglesToConsiderForBranchingVarCalculation =  
                getRectanglesToConsiderForBranchingVarCalculation(   compatibleRectMap);
        
        //collect var refcounts into a treemap
        Map<String, Integer> varCountMap = new HashMap <String, Integer> ();
        
        for (Rectangle rect:rectanglesToConsiderForBranchingVarCalculation) {
            for ( String var :rect.zeroFixedVariables){
                
                //ignore vars that have already been branched upon
                if (leaf.zeroFixedVariables.contains(var)||leaf.oneFixedVariables.contains(var)) continue;
                
                int count = varCountMap.get(var)==null? ZERO : varCountMap.get(var);
                varCountMap.put(var, count+ONE);
            }
            for ( String var :rect.oneFixedVariables){
                                
                //ignore vars that have already been branched upon
                if (leaf.zeroFixedVariables.contains(var)||leaf.oneFixedVariables.contains(var)) continue;
                
                int count = varCountMap.get(var)==null? ZERO : varCountMap.get(var);
                varCountMap.put(var, count+ONE);
            }
        }
        
        int maxFrequency = Collections.max(varCountMap.values());
        //pick any var with this frequency
        for (Map.Entry<String, Integer> entry : varCountMap.entrySet()){
            if (entry.getValue()==maxFrequency) {
                varAndFrequency.coeff=maxFrequency;
                varAndFrequency.varName=entry.getKey();
                break;
            }
        }
        
        return varAndFrequency;
    }
    
    private static List<Rectangle> getRectanglesToConsiderForBranchingVarCalculation( Map<Double, List<Rectangle>  > compatibleRectMap){
        return compatibleRectMap.get ( Collections.min(compatibleRectMap.keySet()) );
    }
}
