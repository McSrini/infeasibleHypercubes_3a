/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a.collection;

import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ONE;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ZERO;
import static ca.mcmaster.infeasiblehypercubes_3a.Parameters.MIP_FILENAME;
import ca.mcmaster.infeasiblehypercubes_3a.common.UpperBoundConstraint;
import ca.mcmaster.infeasiblehypercubes_3a.common.VariableCoefficientTuple;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.*;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tamvadss
 */
public class CplexBasedOriginFinder {
    
    private IloCplex cplex;
    private  IloNumVar[] variables  ;
    private UpperBoundConstraint reducedConstraint;
    
    public double valueAtBestVertex ;
    
    public CplexBasedOriginFinder(UpperBoundConstraint reducedConstraint, 
                                  List <String> zeroFixedVariables, 
                                  List <String> oneFixedVariables) throws IloException{
        
        this.reducedConstraint = reducedConstraint;
        
        //import mip into ilocplex
        cplex = new IloCplex ();
      
        cplex.importModel(MIP_FILENAME);
        
        //remove all constraints
        //remove all constrs
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        variables  =lpMatrix.getNumVars();
        IloRange[] constraints = lpMatrix.getRanges();
        for (IloRange constr: constraints ){
            cplex.delete(constr) ;
        }
        
        
        
        //add ubc
        addConstraint(reducedConstraint);
        //add the var fixings for this rectangle
        addVarFixing(zeroFixedVariables, ZERO) ;
        addVarFixing(oneFixedVariables, ONE) ;
        
        cplex.use (new CplexIncumbentHandler(variables, reducedConstraint)) ;
        
        cplex.setParam(  IloCplex.IntParam.HeurFreq , -ONE);
        cplex.setParam(IloCplex.Param.MIP.Limits.CutPasses,ZERO);        
        cplex.setParam(IloCplex.Param.Preprocessing.Presolve, false);
       
    }
    
    //once solved , get the best vertex 
    //return false if infeasible, true otherwise
    public boolean getVarFixingsAtOrigin (List<Boolean> isVarZeroFixedAtOrigin) throws IloException{
         
        isVarZeroFixedAtOrigin.clear();
        boolean retval = true;
        
        //minimize
        cplex.solve();
        retval= cplex.getStatus().equals(Status.Optimal);
        
        if (retval){
            double[]  values = cplex.getValues(variables);
            //System.out.println("optimal is "+ cplex.getObjValue());

            this.valueAtBestVertex = cplex.getObjValue();

            //create a map of var and value
            Map<String, Double > varValueMap = new HashMap <String, Double> ();
            for (int index = ZERO; index < variables.length; index ++) {
                varValueMap.put( variables[index].getName(), values[index] );
                //System.out.println("Var is "+ variables[index].getName() + " and its value is " + values[index]) ;
            }

            //find var values at optimum vertex
            for (int index = ZERO; index < reducedConstraint.sortedConstraintExpr.size(); index ++) {
                double varValue = varValueMap.get(reducedConstraint.sortedConstraintExpr.get(index).varName);
                isVarZeroFixedAtOrigin.add(Math.round(varValue)==ZERO) ;
            }
        }
        
        return retval ;
    }
    
    private void addVarFixing (List <String> varNames, int value) throws IloException {
        for (String varname: varNames){
             cplex.addEq(getVar (  varname), value);
        }
    }
    
    
    private void addConstraint(UpperBoundConstraint ubc) throws IloException {
        IloNumExpr expr = cplex.linearNumExpr();
        for (VariableCoefficientTuple tuple : ubc.sortedConstraintExpr){
            IloNumVar var = getVar(tuple.varName) ;
            expr = cplex.sum(expr, cplex.prod( tuple.coeff, var));
        }
        cplex.addLe(expr , ubc.upperBound );
         
    }
    
    
    private IloNumVar getVar (String varname){
        IloNumVar result = null;
        for (IloNumVar var : this.variables) {
            if (var.getName().equals( varname)){
                result= var;
                break;
            }
        }
        return result;
    }
}
