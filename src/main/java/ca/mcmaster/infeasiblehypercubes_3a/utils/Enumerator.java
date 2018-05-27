/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a.utils;

import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ZERO;
import ca.mcmaster.infeasiblehypercubes_3a.collection.LeafNode;
import ca.mcmaster.infeasiblehypercubes_3a.collection.Rectangle;
import ca.mcmaster.infeasiblehypercubes_3a.drivers.Base_Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class Enumerator {
        
    private List <String> nodeZeroFixedVariables = new ArrayList <String>();
    private List <String> nodeOneFixedVariables = new ArrayList <String>();
    
    public Enumerator ( LeafNode leaf ) {
         
        this.nodeZeroFixedVariables=leaf.zeroFixedVariables;
        this.nodeOneFixedVariables=leaf.oneFixedVariables;
    }
 
    //if a node has no shared vars among compatible infeasible rects, then enumerated soln possible for this node
    public LeafNode getEnumeratedSolution (  Map<Double, List<Rectangle>  > compatibleRectMap  ) {
        
        //prepare all infeasible rects known
        List<Rectangle>  infeasibleRectangles  = new ArrayList<Rectangle>     ();
        for ( List<Rectangle>  rectList :  compatibleRectMap.values()){
            infeasibleRectangles.addAll(rectList);
        }
        
        //create empty rect
        LeafNode soln = new LeafNode ( new ArrayList<String> () ,  new ArrayList<String> () );
        //add in vars fixed at this node
        soln.zeroFixedVariables.addAll(nodeZeroFixedVariables);
        soln.oneFixedVariables.addAll(nodeOneFixedVariables);
        
        for (Rectangle infeasibleRect:  infeasibleRectangles) {
             
            //find the best var fixings for this infeasible rect, if it coincides with the infeasiblity condition then find second best fixing
             
             Rectangle delta = getEnumeratedSolution (   infeasibleRect);
             soln.zeroFixedVariables.addAll(delta.zeroFixedVariables );
             soln.oneFixedVariables.addAll( delta.oneFixedVariables);
         }
        
        //fix any reamining free vars at their best possible value
        soln.getLpRelaxVertex_Minimization();
        return soln;
         
    }
    
    private Rectangle getEnumeratedSolution (Rectangle infeasibleRect){
        Rectangle bestSolution =  getBestVarFixings(infeasibleRect) ;
        if ( infeasibleRect.oneFixedVariables.containsAll(bestSolution.oneFixedVariables )  &&
             infeasibleRect.zeroFixedVariables.containsAll( bestSolution.zeroFixedVariables)    ) {
            
            //use second best solution, which gauranteed to exist ( if rect is invalid then one variable flipped has to be valid).
            bestSolution =  getSecondBestVarFixings(bestSolution);
            
        }else {
            //use best solution
        }
        
        return bestSolution;       
    }    
    
    //for each variable , if its positive choose it to 0 else to 1
    private Rectangle getBestVarFixings (Rectangle infeasibleRect   ) {
        Rectangle bestSolution =new Rectangle ( new ArrayList<String> () ,  new ArrayList<String> () );
          
        List <String> allVars = new ArrayList<String> ();
        allVars.addAll(  infeasibleRect.zeroFixedVariables );
        allVars.addAll(  infeasibleRect.oneFixedVariables );
        
        for (String var : allVars) {
            
            //if var is fixed by node branching conditions, we cannot choose its value
            if (this.nodeZeroFixedVariables.contains(var) ) {
                continue;
            }
            if (this.nodeOneFixedVariables.contains(var)) {
                continue; 
            }
            
            //if obj coeff > 0 , choose 0 fixing else choose 1 fixing
            double objCoeff = Base_Driver.objective.getObjectiveCoeff(var);
            if (objCoeff>ZERO){
                bestSolution.zeroFixedVariables.add(var);
            }else {
                bestSolution.oneFixedVariables.add(var);
            }
        }
        
        return bestSolution;
    }
    
    //change the free variable with the lowest objective coeff
    private Rectangle getSecondBestVarFixings (Rectangle bestSolution) {
        
        Rectangle secondBestSolution =new Rectangle (bestSolution.zeroFixedVariables, bestSolution.oneFixedVariables);
        
        String varToFlip = getVarWithLowestObjectiveCoeffMagnitude(bestSolution); 
        
        //flip this var in the best solution 
        boolean varFound = false ;
        for (String str : bestSolution.zeroFixedVariables) {
            if (varToFlip.equals(str)){
                varFound = true;
                secondBestSolution.zeroFixedVariables.remove(str);
                secondBestSolution.oneFixedVariables.add(str);
                break;
            }
        }
        for (String str : bestSolution.oneFixedVariables) {
            if (true ==varFound)  break;
            if (varToFlip.equals(str)){
                varFound = true;
                secondBestSolution.oneFixedVariables.remove(str);
                secondBestSolution.zeroFixedVariables.add(str);
                break;
            }
        }
        
        return secondBestSolution;        
    }
    
    //of the vars that define this rectangle, find the one which (when flipped) will result in the smallest change of objective
    private String getVarWithLowestObjectiveCoeffMagnitude (Rectangle rect) {
        String bestVar = null;
        Double bestValSoFar = Double.MAX_VALUE;
        
        List <String> vars = new ArrayList<String> () ;
        vars.addAll( rect.zeroFixedVariables );
        vars.addAll( rect.oneFixedVariables );
        
        //remove all vars which are fixed by branching conditions for this node        
        for (String var: this.nodeZeroFixedVariables) {
            vars.remove(var);
        }
        for (String var : this.nodeOneFixedVariables) {
            vars.remove(var);
        }
                
        for (String  var: vars){
            double thisVal = Base_Driver.objective.getObjectiveCoeff(var);
            if ( Math.abs( thisVal) < Math.abs(bestValSoFar)) {
                bestValSoFar=thisVal;
                bestVar= var;
            }
        }      
        
        return bestVar;
        
    }
}
