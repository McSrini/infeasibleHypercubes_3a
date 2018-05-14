/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.infeasiblehypercubes_3a.utils;

import ca.mcmaster.infeasiblehypercubes_3a.collection.LeafNode;
import ca.mcmaster.infeasiblehypercubes_3a.collection.Rectangle;
import java.util.List;

/**
 *
 * @author tamvadss
 */
public class RectangleFilter {
    
    public RectangleFilter () {
        
    }
    
    //compatible rectangle cannot conflict any var bound on the leaf
    //return value indicates if leaf is infeasible because its branching conditiosn are a superset of one of the feasible rects
    public  boolean  getCompatibleRectangles ( List<Rectangle> rectangleList, LeafNode leaf , List<Rectangle>  compatibleRectList) {
      
        boolean result = false;
        for (Rectangle rect: rectangleList){
            if (checkUNFeasible (rect, leaf )){
                //if leaf is infeasible, we dont care for compatible rect list
                result = true ;
                compatibleRectList.clear();
                break;
            } else if (isCompatible (rect, leaf)) {
                compatibleRectList.add(rect) ;
            }
        }
        
        return result;
    }
    
    //return true if leaf is a ssuperset of rect
    private boolean  checkUNFeasible (  Rectangle  rectangle , LeafNode leaf){        
        
        return leaf .oneFixedVariables.containsAll(rectangle.oneFixedVariables) && leaf.zeroFixedVariables.containsAll(rectangle.zeroFixedVariables);         
    }
    
    //ensure rect has no conflicting bounds
    private boolean isCompatible ( Rectangle  rectangle, LeafNode leaf){
        boolean result = true;
        
        for (String str : rectangle.zeroFixedVariables) {
             if (leaf.oneFixedVariables.contains(str)){
                 //incompatible
                 result = false;
                 break;
             }
        }
        
        if (result) {
            for (String str : rectangle.oneFixedVariables) {
                 if (leaf.zeroFixedVariables.contains(str)){
                     //incompatible
                     result = false;
                     break;
                 }
            }
        }
        
        return result;
    }
    
}
