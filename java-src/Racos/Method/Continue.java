/* This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Copyright (C) 2015 Nanjing University, Nanjing, China
 */
 
 /**
 * Class continue
 * @author Yi-Qi Hu
 * @time 2015.11.14
 * @version 2.0
 * continue class is the method of Racos in continuous feasible region
 */

package Racos.Method;

import Racos.Componet.*;
import Racos.Tools.*;
import Racos.ObjectiveFunction.*;
import java.util.ArrayList;
import java.util.Collections;

public class Continue extends BaseParameters{
	
	private Task task;                   //the task that algorithm will optimize       
	private Dimension dimension;         //the task dimension message that algorithm optimize
	private Instance[] Pop;              //the instance set that algorithm will handle with
	private Instance[] NextPop;          //the instance set that algorithm will generate using instance set Pop
	private Instance[] PosPop;           //the instance set with best objective function value
	private Instance Optimal;            //an instance with the best objective function value
	private Model model;
	private class Model{                 //the model of generating next instance
		
		public double[][] region;//shrinked region
		public boolean[] label;  //if label[i] is false, the corresponding dimension should be randomized from region
		
		public Model(int size){
			region = new double[size][2];
			label = new boolean[size];
			for(int i=0; i<size; i++){
				region[i][0] = 0;
				region[i][1]  =1;
				label[i] = false;
			}
		}
		
		public void PrintLabel(){
			for(int i=0; i<label.length; i++){
				if(!label[i]){
					System.out.print(i+" ");
				}
			}
			System.out.println();
		}
	}
	
	/**
	 * constructors function
	 * user must constructs class Continue with a class which implements interface Task
	 * 
	 * @param ta  the class which implements interface Task
	 */
	public Continue(Task ta){
		task = ta;
		dimension = ta.getDim();
	}
	
	//the next several functions are prepared for testing
	public void PrintPop(){
		System.out.println("Pop set:");
		if(Pop.length==0){
			System.out.println("Pop set is null!");
		}
		for(int i=0; i<Pop.length; i++){
			Pop[i].PrintInstance();
		}
	}
	public void PrintPosPop(){
		System.out.println("PosPop set:");
		if(PosPop.length==0){
			System.out.println("PosPop set is null!");
		}
		for(int i=0; i<PosPop.length; i++){
			PosPop[i].PrintInstance();
		}
	}
	public void PrintNextPop(){
		System.out.println("NextPop set:");
		if(NextPop.length==0){
			System.out.println("NextPop set is null!");
		}
		for(int i=0; i<NextPop.length; i++){
			NextPop[i].PrintInstance();
		}
	}
	public void PrintOptimal(){
		System.out.println("Optimal:");
		Optimal.PrintInstance();
	}
	public void PrintRegion(){
		System.out.println("Region:");
		for(int i=0; i<model.region.length; i++){
			System.out.print("["+model.region[i][0]+","+model.region[i][1]+"] ");
		}
		System.out.println();
	}
	
	/**
	 * 
	 * @return the optimal that algorithm found
	 */
	public Instance getOptimal(){
		return Optimal;
	}
	
	/**
	 * RandomInstance without parameter
	 * 
	 * @return an Instance, each feature in this instance is a random number from original feasible region
	 */
	protected Instance RandomInstance(){
		Instance ins = new Instance(dimension);
		RandomOperator ro = new RandomOperator();//tool of randomizing
		for(int i=0; i<dimension.getSize(); i++){
			
			if(dimension.getType(i)){//if i-th dimension type is continue
				ins.setFeature(i, ro.getDouble(dimension.getRegion(i)[0], dimension.getRegion(i)[1]));
			}else{//if i-th dimension type is discrete
				ins.setFeature(i, ro.getInteger((int)dimension.getRegion(i)[0], (int)dimension.getRegion(i)[1]));
			}
			
		}
		return ins;
	}
	
	/**
	 * Random instance with parameter
	 * 
	 * @param pos, the positive instance that generate the model
	 * @return an Instance, each feature in this instance is a random number from a feasible region named model
	 */
	protected Instance RandomInstance(Instance pos){
		Instance ins = new Instance(dimension);
		RandomOperator ro = new RandomOperator();//tool of randomizing
//		model.PrintLabel();
		for(int i=0; i<dimension.getSize(); i++){
			
			if(dimension.getType(i)){//if i-th dimension type is continue
				if(model.label[i]){//according to fixed dimension, valuate using corresponding value in pos
					ins.setFeature(i, pos.getFeature(i));
				}else{//according to not fixed dimension, random in region
//					System.out.println("["+model.region[i][0]+", "+model.region[i][1]+"]");
					ins.setFeature(i, ro.getDouble(model.region[i][0], model.region[i][1]));
				}
			}else{//if i-th dimension type is discrete
				if(model.label[i]){//according to fixed dimension, valuate using corresponding value in pos
					ins.setFeature(i, pos.getFeature(i));
				}else{//according to not fixed dimension, random in region
					ins.setFeature(i, ro.getInteger((int)model.region[i][0], (int)model.region[i][1]));
				}
			}
			
		}
		return ins;
	}
	
	/**
	 * initialize Pop, NextPop, PosPop and Optimal
	 * 
	 */
	@SuppressWarnings("unchecked")
	protected void Initialize(){
		
		Instance[] temp = new Instance[SampleSize+PositiveNum];
		
		Pop = new Instance[SampleSize];
		
		//sample Sample+PositiveNum instances and add them into temp
		for(int i=0; i<SampleSize+PositiveNum; i++){
			temp[i] = RandomInstance();
			temp[i].setValue(task.getValue(temp[i]));
		}	
				
		//sort Pop according to objective function value
		InstanceComparator comparator = new InstanceComparator();
		java.util.Arrays.sort(temp,comparator);
		
		//initialize Optimal
		Optimal = temp[0].CopyInstance();
		
		//after sorting, the beginning several instances in temp are used for initializing PosPop
		PosPop = new Instance[PositiveNum];
		for(int i=0; i<PositiveNum; i++){
			PosPop[i] = temp[i];
		}
		
		Pop = new Instance[SampleSize];
		for(int i=0; i<SampleSize; i++){
			Pop[i] = temp[i+PositiveNum];
		}
		
		//initialize NextPop
		NextPop = new Instance[SampleSize];
		
		model = new Model(dimension.getSize());
		
		return ;
		
	}
	
	/**
	 * 
	 * @return the model with original feasible region and all label is true
	 */
	protected void ResetModel(){
		for(int i=0; i<dimension.getSize(); i++){
			model.region[i][0] = dimension.getRegion(i)[0];	
			model.region[i][1] = dimension.getRegion(i)[1];	
			model.label[i] = false;
		}
		return ;
	}
	
	/**
	 * shrink model for instance pos
	 * 
	 * @param pos
	 */
	protected void ShrinkModel(Instance pos){
		RandomOperator ro = new RandomOperator();
		int ChoosenDim;  
		int ChoosenNeg;  
		double tempBound;
		
		while (!Distinguish()) {//generate the model
			ChoosenDim = ro.getInteger(0, dimension.getSize() - 1);//choose a dimension randomly
			ChoosenNeg = ro.getInteger(0, this.SampleSize - 1);    //choose a negative instance randomly
			// shrink model
			if (pos.getFeature(ChoosenDim) < Pop[ChoosenNeg].getFeature(ChoosenDim)) {
				tempBound = ro.getDouble(pos.getFeature(ChoosenDim), Pop[ChoosenNeg].getFeature(ChoosenDim));
				if (tempBound < model.region[ChoosenDim][1]) {
					model.region[ChoosenDim][1] = tempBound;

				}
			} else {
				tempBound = ro.getDouble(Pop[ChoosenNeg].getFeature(ChoosenDim), pos.getFeature(ChoosenDim));
				if (tempBound > model.region[ChoosenDim][0]) {
					model.region[ChoosenDim][0] = tempBound;
				}
			}
		}
		return ;
		
	}
	
	/**
	 * make sure that the number of random dimension is smaller than the threshold UncertainBits
	 * 
	 * @param model
	 * @return
	 */
	protected void setRandomBits(){
		RandomOperator ro = new RandomOperator();
		int labelMarkNum;
		int[] labelMark = new int[dimension.getSize()];
		int tempLab;
		labelMarkNum = dimension.getSize();
		for(int k=0; k<dimension.getSize(); k++){
			labelMark[k] = k;
		}
		for (int k = 0; k < dimension.getSize()-UncertainBits; k++) {						
			tempLab = ro.getInteger(0, labelMarkNum-1);
			model.label[labelMark[tempLab]] = true;
			labelMark[tempLab] = labelMark[labelMarkNum-1];
			labelMarkNum--;
		}
		return ;
		
	}
	
	/**
	 * if each instance in Pop is not in model, return true; if exist more than one instance in Pop is in the model, return false
	 * 
	 * @param model
	 * @return true or false
	 */
	protected boolean Distinguish(){
		int j;
		for(int i=0;i<this.SampleSize; i++){
			for(j=0; j<dimension.getSize(); j++){
				if(Pop[i].getFeature(j)>model.region[j][0]&&Pop[i].getFeature(j)<model.region[j][1]){
					
				}else{
					break;
				}
			}
			if(j==dimension.getSize()){
				return false;
			}
		}
		return true;		
	}
	
	/**
	 * judge whether the instance is in the region of model, if instance is in the model, return true, else return true
	 * @param model, the model
	 * @param ins, the instance
	 * @return
	 */
	protected boolean InstanceIsInModel(Model model, Instance ins){
		for(int i=0; i<ins.getFeature().length; i++){
			if(ins.getFeature(i)>model.region[i][1]||ins.getFeature(i)<model.region[i][0]){
				return false;//if i-th dimension is not in the region, return false
			}
		}
		return true;
	}
	
	/**
	 * if ins exist in Pop, PosPop, NextPop, return true; else return false
	 * @param ins
	 * @return
	 */
	protected boolean notExistInstance(int end, Instance ins){
		int i,j;
		for(i=0; i<this.PositiveNum;i++){
			if(ins.Equal(PosPop[i])){
				return false;
			}
		}
		for(i=0;i<this.SampleSize;i++){
			if(ins.Equal(Pop[i])){
				return false;
			}
		}
		for(i=0;i<end;i++){
			if(ins.Equal(NextPop[i])){
				return false;
			}
		}
		return true;
		
	}
	
	/**
	 * update set PosPop using NextPop
	 * 
	 */
	protected void UpdatePosPop(){
		Instance TempIns = new Instance(dimension);
		int j;
		for(int i=0; i<this.SampleSize; i++){
			for(j=0; j<this.PositiveNum; j++){
				if(PosPop[j].getValue()>Pop[i].getValue()){
					break;
				}
			}
			if(j<this.PositiveNum){
				TempIns=Pop[i];
				Pop[i]=PosPop[this.PositiveNum-1];
				for(int k=this.PositiveNum-1; k>j;k--){
					PosPop[k]=PosPop[k-1];
				}
				PosPop[j]=TempIns;
			}
		}
		return ;
	}
	
	/**
	 * update optimal
	 */
	protected void UpdateOptimal(){
		if (Optimal.getValue() > PosPop[0].getValue()) {
			Optimal=PosPop[0];
		}
		return ;
	}
	
	/**
	 * after setting parameters of Racos, user call this function can obtain optimal
	 * 
	 */
	public void run(){
		
		int ChoosenPos;
		double GlobalSample;
		boolean reSample;
		RandomOperator ro = new RandomOperator();
		
		model = new Model(dimension.getSize());
		
		ResetModel();
		Initialize();
		
		for(int i=1; i<this.MaxIteration; i++){		
			for(int j=0; j<this.SampleSize; j++){

				reSample = true;
				while (reSample) {		
					ResetModel();
					ChoosenPos = ro.getInteger(0, this.PositiveNum - 1);
					GlobalSample = ro.getDouble(0, 1);
					if (GlobalSample >= this.RandProbability) {
					} else {

						ShrinkModel(PosPop[ChoosenPos]);//shrinking model

						setRandomBits();//set uncertain bits
						
					}
					NextPop[j] = RandomInstance(PosPop[ChoosenPos]);//sample
					if (notExistInstance(j, NextPop[j])) {
						NextPop[j].setValue(task.getValue(NextPop[j]));//query
						reSample = false;
					}
				}
			}
			
			//copy NextPop
			for (int j = 0; j < this.SampleSize; j++) {
				Pop[j] = NextPop[j];
			}
			
			UpdatePosPop();//update PosPop set
			
			UpdateOptimal();//obtain optimal

		}
		return ;
		
	}
	
	
	

}
