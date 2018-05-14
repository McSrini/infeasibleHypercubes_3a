/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a.collection;

import ca.mcmaster.infeasiblehypercubes_3a.drivers.Base_Driver;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ONE;
import static ca.mcmaster.infeasiblehypercubes_3a.Constants.ZERO;
import ca.mcmaster.infeasiblehypercubes_3a.common.VariableCoefficientTuple;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tamvadss
 */
public class LeafNode extends Rectangle {
    //tree nodes have unique IDs
    public static long NODE_ID = ZERO;    
    public long myId =-ONE;
    
    public double lpRelaxValueMinimization;     
    public List <String> lpVertex_zeroFixedVariables = new ArrayList <String>();
    public List <String> lpVertex_oneFixedVariables  = new ArrayList <String>();
    
    
    public LeafNode ( List <String> zeroFixed,  List <String> oneFixed){
        super( zeroFixed,   oneFixed) ;
        myId =NODE_ID++;
    }
    
    //get min possible value
    public double getLpRelaxVertex_Minimization () {
        
        this.lpRelaxValueMinimization = ZERO;
        this.lpVertex_oneFixedVariables.clear();
        this.lpVertex_zeroFixedVariables.clear();
        
        for (VariableCoefficientTuple tuple: Base_Driver.objective.objectiveExpr){
            if (this.oneFixedVariables.contains(tuple.varName) ){
                this.lpRelaxValueMinimization+=tuple.coeff;
                lpVertex_oneFixedVariables.add(tuple.varName) ;
            }else if (this.zeroFixedVariables.contains(tuple.varName)) {
                lpVertex_zeroFixedVariables.add(tuple.varName);
            }
            
            if (!this.oneFixedVariables.contains(tuple.varName) && !this.zeroFixedVariables.contains(tuple.varName)   ){
                //free var
                if (tuple.coeff<ZERO) {
                    this.lpRelaxValueMinimization+=tuple.coeff;
                    lpVertex_oneFixedVariables.add(tuple.varName) ;
                }else {
                    lpVertex_zeroFixedVariables.add(tuple.varName);
                }
            }
        }
        
        return lpRelaxValueMinimization;
    }
}
