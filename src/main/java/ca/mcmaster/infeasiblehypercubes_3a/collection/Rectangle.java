/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a.collection;

import ca.mcmaster.infeasiblehypercubes_3a.drivers.Base_Driver;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ONE;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ZERO;
import static ca.mcmaster.infeasiblehypercubes_3a.Parameters.USE_STRICT_INEQUALITY_IN_MIP;
import ca.mcmaster.infeasiblehypercubes_3a.collection.CplexBasedOriginFinder;
import ca.mcmaster.infeasiblehypercubes_3a.common.UpperBoundConstraint;
import ca.mcmaster.infeasiblehypercubes_3a.common.VariableCoefficientTuple;
import ilog.concert.IloException;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tamvadss
 */
public class Rectangle {
    
    
    //note that some vars can be free
    public List <String> zeroFixedVariables = new ArrayList <String>();
    public List <String> oneFixedVariables = new ArrayList <String>();
     
    public double bestVertexValue;     
    //which vars are zero at best vertex? order is the same as in reduced constraint
    List<Boolean> isVariableFixedAtZeroAtBestVertex = new ArrayList<Boolean>();  
    //constraint value at best vertex
    public double reducedConstraint_ValueAtBestVertex ; 
        
    //public List <String> bestVertex_zeroFixedVariables = new ArrayList <String>();
    //public List <String> bestVertex_oneFixedVariables  = new ArrayList <String>();
      
    public Rectangle (List <String> zeroFixedVariables , List <String> oneFixedVariables ){
        this.zeroFixedVariables .addAll(zeroFixedVariables);
        this.oneFixedVariables  .addAll( oneFixedVariables);
      
    }
    
    //get the best vertex ignoring the constraint, if feasible to UBC then its the best vertex
    //else use cplex to find the best vertex
    //
    //return true if feasible and best vertex found, else return false
    public boolean findBestVertex  (UpperBoundConstraint ubc ) throws IloException {
       
        UpperBoundConstraint reducedConstraint = ubc.getReducedConstraint(zeroFixedVariables, oneFixedVariables);
        
        //first ignore the constraint and find origin     (aka best vertex)
        //also find value at origin
        double valueAtBestVertex_IngoringConstraint = ZERO;
        for (VariableCoefficientTuple tuple: reducedConstraint.sortedConstraintExpr) {
            double objCoeff = Base_Driver.objective.getObjectiveCoeff(tuple.varName);
            if (objCoeff< ZERO) {
                isVariableFixedAtZeroAtBestVertex.add(false);   
                //valueAtBestVertex_IngoringConstraint+=objCoeff;
            }else {
                isVariableFixedAtZeroAtBestVertex.add(true);
            }            
        }
        //value at best vertex needs to be augmented using 1 fixed vars for this rect, and also free vars
        for (String rectOneFixedVar : oneFixedVariables){
            valueAtBestVertex_IngoringConstraint +=Base_Driver.objective.getObjectiveCoeff(rectOneFixedVar );
        }
        for (String freeVar : Base_Driver.allVariablesInModel) {
            if(!this.oneFixedVariables.contains(freeVar) &&! this.zeroFixedVariables.contains(freeVar)){
                //free var
                double objCoeff = Base_Driver.objective.getObjectiveCoeff(freeVar);
                if (objCoeff < ZERO) valueAtBestVertex_IngoringConstraint +=objCoeff;
            }
        }
        
        
        //if this vertex is feasible, no need to use cplex
        boolean isFeasible =this.checkIfBestVertex_FeasibleTo_UBConstraint(!USE_STRICT_INEQUALITY_IN_MIP, reducedConstraint);
         
        if (! isFeasible){             
            //use cplex to get origin
                           
            CplexBasedOriginFinder finder = new CplexBasedOriginFinder ( reducedConstraint,  zeroFixedVariables,  oneFixedVariables) ;
            isFeasible=finder.getVarFixingsAtOrigin(this.isVariableFixedAtZeroAtBestVertex);
            if(isFeasible){
                //set the value of the best vertex found
                bestVertexValue = finder.valueAtBestVertex;
            }
        }else {
            bestVertexValue =valueAtBestVertex_IngoringConstraint;
        }
        
        //check if still not feasible 
        
        if (isFeasible){
            
            //find the constraint value at best vertex
            this.reducedConstraint_ValueAtBestVertex= this.getReducedConstraintValueAtBestVertex(reducedConstraint );
            
            //print the var fixings at best vertex
            int index = ZERO;
            for (VariableCoefficientTuple tuple: reducedConstraint.sortedConstraintExpr) {
                if (isVariableFixedAtZeroAtBestVertex.get(index)) {
                    System.out.println ("var fixed at zero at best vertex "+tuple.varName) ;
                }else {
                    System.out.println ("var fixed at one at best vertex "+tuple.varName) ;
                }
                index++;
            }
            
        }
        
        
        //if still not feasible  , return false, else return true
        return isFeasible;
        
                 
    }
    
    public String toString (){
        String result=" ";//lp realx " + this.lpRelaxValueMinimization;
        result += " --- Zero fixed vars :";
        for (String str: zeroFixedVariables){
            result += str + ",";
        }
        result += "  -- One fixed vars :";
        for (String str: oneFixedVariables){
            result += str + ",";
        }
        return result;
    }
    
    private double getReducedConstraintValueAtBestVertex ( UpperBoundConstraint reducedConstraint ){
        double value = ZERO ;
        int index = ZERO;
        for (VariableCoefficientTuple tuple: reducedConstraint.sortedConstraintExpr) {
             if (!isVariableFixedAtZeroAtBestVertex.get(index)) {
                 value += tuple.coeff;
             }
             index ++;
        }
        return value ;
    }
    
    private boolean checkIfBestVertex_FeasibleTo_UBConstraint (boolean isStrict,  UpperBoundConstraint reducedConstraint){         
        double constraintValueAtBestVertex =getReducedConstraintValueAtBestVertex (   reducedConstraint );
        return isStrict ? (constraintValueAtBestVertex < reducedConstraint.upperBound) : (constraintValueAtBestVertex <=reducedConstraint.upperBound);
    }
    
}
