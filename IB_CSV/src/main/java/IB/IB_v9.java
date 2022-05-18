package IB;

//v1- Plots IndexBands with color set in UI. Uses HistoricalDataListener.
//v2- Same as v1 except that it............. DOES NOT Use HistoricalDataListener.
//v3- Had to delete v3. KEpt getting failed to enumerate entry points errors.
//v3- I took copy of v2 and v4 and named v3 and they still failed???
//v4- New color settings. Compiles and plots but new colors from CSV signal still Don't work.
//v5- copy of v4 for more testing. This had more complex color settings for later use after i figure out how to Double.NaN after change
//v6- copy of v2. Added Arrays to store previous values
//v7- same as v6!! Added Double.NaN to stop plotting. USE THIS!!!
//v8- copy of v7. Added more complex color settings from v5.
//v9- Changed Symbol String for CSV File. Added Widget Rules

 
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
//import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
//import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Stream;
//import java.util.Iterator;
import javax.swing.JComboBox;
import javax.swing.JLabel;


import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.LayerRenderPriority;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
//import velox.api.layer1.simplified.HistoricalDataListener;
//import velox.api.layer1.simplified.HistoricalModeListener;
import velox.api.layer1.simplified.Indicator;
//import velox.api.layer1.simplified.IndicatorModifiable;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.IntervalListener;
import velox.api.layer1.simplified.Intervals;
import velox.api.layer1.simplified.LineStyle;
import velox.api.layer1.settings.StrategySettingsVersion;
import velox.api.layer1.simplified.TimeListener;
import velox.gui.StrategyPanel;
import velox.gui.colors.ColorsConfigItem;
import velox.api.layer1.simplified.CustomSettingsPanelProvider;
import com.bookmap.api.simple.demo.utils.gui.BookmapSettingsPanel;


@Layer1SimpleAttachable
@Layer1StrategyName("IB_v9")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class IB_v9 implements CustomModule, IntervalListener/*, HistoricalDataListener*/, TimeListener, CustomSettingsPanelProvider {
   
     @StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})	  	
     public static class Settings {
    	 public int cUpdateInt = defaultUpdateInterval;
	     public Color colorBull = defBullc;
	     public Color colorBear = defBearc;
	     public Color colorMBull = defMBullc;
	     public Color colorMBear = defMBearc;	     

	     public int lineBWidth = defBLineWidth;
	     public int lineMBWidth = defMBLineWidth;      
	     	     
	     public LineStyle lineBStyle = defBLineStyle;
	     public LineStyle lineMBStyle = defMBLineStyle;	     
		      
	     public boolean reloadOnChange = true;
    } 
    
	protected Settings settings;
    protected Api api;
 	
	protected Indicator bullBandH;
	protected Indicator bullBandL;
	protected Indicator MbullBandH;
	protected Indicator MbullBandL;	
	
	protected Indicator bearBandH;
	protected Indicator bearBandL;
	protected Indicator MbearBandH;
	protected Indicator MbearBandL;		
		
    
    private static final Color defBullc =  Color.BLUE;
    private static final Color defMBullc = Color.BLUE.darker().darker().darker();
    private static final Color defBearc =  Color.RED;
    private static final Color defMBearc = Color.RED.darker().darker().darker();
	
	
	private int minimalIntervalCount;
    final long minimalInterval= Intervals.INTERVAL_5_SECONDS;  
    private long nCustomInterval;
	private static final int defaultUpdateInterval = 15;	
    
    private static final int defBLineWidth = 6;     
    private static final int defMBLineWidth = 4;       
    
    private static final LineStyle defBLineStyle = LineStyle.SOLID;
    private static final LineStyle defMBLineStyle = LineStyle.SOLID;    
    

    Double pipV  = 0.0;
    private long t;
 	protected String TimeStr; 
 	 
 	private Double Price = null;	
 	//private Boolean newPlotprice; 
 			
 	private volatile String symStr = ""; 				// should this be static and volatile?
 	//private volatile String newStr = "";			    // should this be private only?
 	private volatile String finalfilepath;				// should this be private only?

 	private int[] bhTimeArray = new int[3];
	private int[] mbhTimeArray = new int[3];

 	private int[] blTimeArray = new int[3];
	private int[] mblTimeArray = new int[3];

		
	 private void setBVisualProperties(final Indicator indicator) {
	   indicator.setLineStyle(this.settings.lineBStyle);
	   indicator.setWidth(this.settings.lineBWidth);    
	 }
		
	 private void setMBVisualProperties(final Indicator indicator) {
	   indicator.setLineStyle(this.settings.lineMBStyle);
	   indicator.setWidth(this.settings.lineMBWidth);          
	 }
	
	 
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {		
		Log.info("Initialize " + symStr);
		this.api = api;
		this.settings = api.getSettings(Settings.class);		      
        nCustomInterval = settings.cUpdateInt;
        		
        this.symStr = info.symbol.toString();					//added 'this.'
		//this.newStr = symStr.substring(0, symStr.length() - 2); //added 'this.' which seems to fix newStr from ging string value from second subscription 		 		
		this.finalfilepath = "C:\\Bookmap\\Notes\\BookMap_IB_ADDon_" + symStr + ".csv"; 	// How do i chk if file exists?				 
        
		this.pipV = info.pips;

		//priority = 600 seems to be bid/ask dot level
		this.bullBandH = api.registerIndicator("BullBH",GraphType.PRIMARY,Double.NaN,true,true);//added 'this.'
	    setBVisualProperties(bullBandH);											 //should i add 'this.' to these also?
	    bullBandH.setColor(defBullc);											 //should i add 'this.' to these also?
	    bullBandH.setRenderPriority(LayerRenderPriority.TOP.priority = 560 );   	   	
	    
		this.bullBandL = api.registerIndicator("BullBL",GraphType.PRIMARY,Double.NaN,true,true);//added 'this.'
	    setBVisualProperties(bullBandL);											 //should i add 'this.' to these also?
	    bullBandL.setColor(defBullc);		
	    bullBandL.setRenderPriority(LayerRenderPriority.TOP.priority = 560 );   
	    
		this.MbullBandH = api.registerIndicator("mBullBH",GraphType.PRIMARY,Double.NaN,true,true);//added 'this.'
	    setMBVisualProperties(MbullBandH);											 //should i add 'this.' to these also?
	    MbullBandH.setColor(defMBullc);											 //should i add 'this.' to these also?
	    MbullBandH.setRenderPriority(LayerRenderPriority.TOP.priority = 550 );   
	    
		this.MbullBandL = api.registerIndicator("mBullBL",GraphType.PRIMARY,Double.NaN,true,true);//added 'this.'
	    setMBVisualProperties(MbullBandL);											 //should i add 'this.' to these also?
	    MbullBandL.setColor(defMBullc);	 
	    MbullBandL.setRenderPriority(LayerRenderPriority.TOP.priority = 550 );   
	    
		
		this.bearBandH = api.registerIndicator("BearBH",GraphType.PRIMARY,Double.NaN,true,true);//added 'this.'
	    setBVisualProperties(bearBandH);											 //should i add 'this.' to these also?
	    bearBandH.setColor(defBearc);											 //should i add 'this.' to these also?
	    bearBandH.setRenderPriority(LayerRenderPriority.TOP.priority = 560 );   
	    
		this.bearBandL = api.registerIndicator("BearBL",GraphType.PRIMARY,Double.NaN,true,true);//added 'this.'
	    setBVisualProperties(bearBandL);											 //should i add 'this.' to these also?
	    bearBandL.setColor(defBearc);		
	    bearBandL.setRenderPriority(LayerRenderPriority.TOP.priority = 560 );   
	    
		this.MbearBandH = api.registerIndicator("mBearBH",GraphType.PRIMARY,Double.NaN,true,true);//added 'this.'
	    setMBVisualProperties(MbearBandH);											 //should i add 'this.' to these also?
	    MbearBandH.setColor(defMBearc);											 //should i add 'this.' to these also?
	    MbearBandH.setRenderPriority(LayerRenderPriority.TOP.priority = 550 );   
	    
		this.MbearBandL = api.registerIndicator("mBearBL",GraphType.PRIMARY,Double.NaN,true,true);//added 'this.'
	    setMBVisualProperties(MbearBandL);											 //should i add 'this.' to these also?
	    MbearBandL.setColor(defMBearc);	 	    
	    MbearBandL.setRenderPriority(LayerRenderPriority.TOP.priority = 550 );   
	    
	  //  try {
			readCsv(finalfilepath) ;
		//} catch (IOException e) {
		//	// TODO Auto-generated catch block
		//	e.printStackTrace();
		//} catch (Exception e) {
		//	// TODO Auto-generated catch block
		//	e.printStackTrace();
		//}	
   }
 
    /*static*/ void readCsv(String nfilePath) /*throws Exception*/ {
		BufferedReader reader = null;
		
		  try {			  
			 // Log.info("readCSV " + symStr + " " + nfilePath);
			  List<PlotVals> plotV = new ArrayList<PlotVals>();
			  String line = "";
			  reader = new BufferedReader(new FileReader(nfilePath));
			  reader.readLine();
		   
			 while((line = reader.readLine()) != null) {
				 String[] fields = line.split(",");
				  
				  if(fields.length > 0) {					 
					  	PlotVals plotT = new PlotVals();
					  	plotT.setSymbol(fields[0]);					  	
					  	plotT.setPLevel(fields[1]);
					  	plotT.setPName(fields[2]);					  	
					  	plotT.setStartTime(fields[3]);
					  	plotT.setPTrend(fields[4]);	  
				 		 
					  	plotV.add(plotT);
					  	//.info("GotCSVDATA");
				  }
			 }
			 
			for(PlotVals p: plotV) {		
				//Log.info("ReadCSV- " + symStr +  " = " + p.getSymbol() + " PLevel " +  p.getPLevel() + " PName " + p.getPName() + " StartTime " + p.getStartTime() + " PTrend " + p.getPTrend());
				compiledata(p.getPName(),p.getPLevel(), p.getStartTime(),p.getPTrend() );
				
				//System.out.printf("[symbol=%s, pricelevel=%s, name=%s, starttime=%s, ptrend=%s]\n", p.getSymbol(), p.getPLevel(), p.getPName(), p.getStartTime(), p.getPTrend());
			}		   
		  } 
		  catch (Exception ex) {
		 	  Log.info("Exception ex1 |" + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");			  
			  ex.printStackTrace();
		  } 
		  finally {
			  try {
				  reader.close();
				  //Log.info("!++++++++++++++++++" + symStr + " Finished Loop +++++++++++++++++++!");
				  //addPoints(localT);	//Is this ok here?
			  } 
			  catch (Exception e) {
				  Log.info("Exception ex2 |" + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				  e.printStackTrace();			  
			  }
		  }		  
	}
    
    /*
   	 public static void main(String[] args){
   		 Log.info("MAin " + symStr); 
   		
   		 readCsv(finalfilepath) ;		  	
   	 }
   */
	
	@Override
		public void onTimestamp(long t) {
		   this.t = t; 		  
	}	
   	 
/*	 
	 class PlotData{  
		 //https://www.javatpoint.com/java-arraylist
		  String pStr;  
		  int pLevel;  
		  int pTime;  
		  int pTrend;  		  
		  
		  PlotData(String pStr,int pLevel, int pTime,int pTrend){  
		   this.pStr=pStr;  
		   this.pLevel=pLevel;  
		   this.pTime=pTime;  		   
		   this.pTrend=pTrend;  
		   
		  }  
	 }  	 
	 
	 class pArrayList{  
		 public void Test(){  
		  //Creating user-defined class objects  
		  PlotData p1=new PlotData("Sonoo",101,23,0);  
		  PlotData p2=new PlotData("Ravi",102,21,0);  
		  PlotData p3=new PlotData("Hanumat",103,25,0);  
		  PlotData p4=new PlotData("Hanumat",103,25,0);  	
		  
		  //creating arraylist  
		  ArrayList<PlotData> al=new ArrayList<PlotData>();  
		  al.add(p1);//adding PlotData class object  
		  al.add(p2);  
		  al.add(p3);  
		  al.add(p4);
		  //Getting Iterator  
		  Iterator<PlotData> itr=al.iterator();  
		  //traversing elements of ArrayList object  
		  while(itr.hasNext()){  
		    PlotData st=(PlotData)itr.next();  
		    System.out.println(st.pStr+" "+st.pLevel+" "+st.pTime+" "+st.pTrend);  
		  }  
		 }  
	 }   
*/
   	 
   	 
   	public LocalDateTime localT() {	
   		return LocalDateTime.ofInstant(Instant.ofEpochSecond(t / 1_000_000_000L), TimeZone.getDefault().toZoneId());	   	  
   	}
   	
   	
   	/*    
   	 @Override
   	 public void onBbo(int bidPrice, int bidSize, int askPrice, int askSize) {
   		   	   	
   	 }  
   	 */	
   		    
   	/*
   	 @Override
   	 public void onTrade(double price, int size, TradeInfo tradeInfo){
   	
   	 }   
   	 */
   	  
	 @Override
	 public long getInterval() {
	    // return Intervals.INTERVAL_15_SECONDS;
	     return minimalInterval;
	 }

	 @Override
	 public void onInterval() {
		//LocalDateTime tStamp = localT();
	 	long customInterval = (long) nCustomInterval * 1_000_000_000L;//seconds to nanos ?
	 	
	 	  if (customInterval != 0 //customInterval is valid
	                && customInterval % minimalInterval == 0 // customInterval is a multiple of minimalInterval 
	                && minimalIntervalCount % (customInterval/minimalInterval) == 0 // minimalIntervalCount is a multiple of custom/minimal ratio
	                ) {
	 		   	  
	 		  //Log.info("++++++++++++++++++"+ symStr +" begin Loop Thru at onInterval+++++++++++++++++");			
	 		  //Log.info("onINterval- "  + symStr + " Time is "  + tStamp.toString());	
			
	 		  try {
				readCsv(finalfilepath) ;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			 	 
	 	  }			
	      minimalIntervalCount++;	
	 }  

     public static String getStrings(String N) {
    	 String plotStr; 
 		if (N.indexOf("/") != -1 ) {
			plotStr = N.substring(0,N.indexOf("/")); 
		}else{
			plotStr = N.toString(); 
		}		 
 		//Log.info("getStrings " + plotStr);
    	return plotStr;
     }

	 public static LocalTime getTimes(String Stime) {			 
		LocalTime TempTime = LocalTime.parse(Stime);	
		LocalTime TimeS = LocalTime.from(TempTime).minusMinutes(1);	
		//Log.info("getTimes- " + " |TimeS " + TimeS);
		
		return TimeS;		
	 }		
	 

	 public Boolean setPrevBHtime(LocalTime Btime) {
		 //ArrayList<String> arlist = new ArrayList<String>( );
		 Integer BandH = Btime.getHour();
		 Integer BandM = Btime.getMinute();		
		 Integer BandS = Btime.getSecond();			 
		 Boolean isNewBand = false;
		 //Log.info("pre BcHECK-    " + " |BandH " + BandH + " |BandM " + BandM + " |BandS " + BandS);	 
		 
		 if(!this.bhTimeArray.equals(null)) {
			 Integer prevBandH = this.bhTimeArray[0] ;		 
			 Integer prevBandM = this.bhTimeArray[1] ;	
			 Integer prevBandS = this.bhTimeArray[2] ;			 
			 //Log.info("setPrevBtime |" + LocalTime.of(prevBandH, prevBandM, prevBandS).toString());
			
			//Log.info("BcHECK-            " + " | Btime " + Btime + " | prevTime " + LocalTime.of(prevBandH, prevBandM, prevBandS));
			if(Btime.compareTo(LocalTime.of(prevBandH, prevBandM, prevBandS)) != 0) {
				isNewBand = true;
			}else {
				isNewBand = false;				
			}
			 //Log.info("isNewBand-       |" + isNewBand.toString() + " | prevBandH " + prevBandH + " | prevBandM " + prevBandM + " | prevBandS " + prevBandS);
		 }
	 
		 this.bhTimeArray[0] = BandH;		 	
		 this.bhTimeArray[1] = BandM;	   
		 this.bhTimeArray[2] = BandS;
		 
		 //Log.info("post BCheck-    |" + isNewBand.toString() + " | bhTimeArray " + bhTimeArray[0] + " | bhTimeArray " + bhTimeArray[1] + " | bhTimeArray " + bhTimeArray[2]);		
		
		return isNewBand;		
	 }	

	 public Boolean setPrevBLtime(LocalTime Btime) {
		 //ArrayList<String> arlist = new ArrayList<String>( );
		 Integer BandH = Btime.getHour();
		 Integer BandM = Btime.getMinute();		
		 Integer BandS = Btime.getSecond();			 
		 Boolean isNewBand = false;
		 //Log.info("pre BcHECK-    " + " |BandH " + BandH + " |BandM " + BandM + " |BandS " + BandS);	 
		 
		 if(!this.blTimeArray.equals(null)) {
			 Integer prevBandH = this.blTimeArray[0] ;		 
			 Integer prevBandM = this.blTimeArray[1] ;	
			 Integer prevBandS = this.blTimeArray[2] ;			 
			 //Log.info("setPrevBtime |" + LocalTime.of(prevBandH, prevBandM, prevBandS).toString());
			
			//Log.info("BcHECK-            " + " | Btime " + Btime + " | prevTime " + LocalTime.of(prevBandH, prevBandM, prevBandS));
			if(Btime.compareTo(LocalTime.of(prevBandH, prevBandM, prevBandS)) != 0) {
				isNewBand = true;
			}else {
				isNewBand = false;				
			}
			 //Log.info("isNewBand-       |" + isNewBand.toString() + " | prevBandH " + prevBandH + " | prevBandM " + prevBandM + " | prevBandS " + prevBandS);
		 }
	 
		 this.blTimeArray[0] = BandH;		 	
		 this.blTimeArray[1] = BandM;	   
		 this.blTimeArray[2] = BandS;
		 
		 //Log.info("post BCheck-    |" + isNewBand.toString() + " | blTimeArray " + blTimeArray[0] + " | blTimeArray " + blTimeArray[1] + " | blTimeArray " + blTimeArray[2]);		
		
		return isNewBand;		
	 }	 
	 
	 public Boolean setPrevMBHtime(LocalTime MBtime) {
		 //ArrayList<String> arlist = new ArrayList<String>( );
		 Integer mBandH = MBtime.getHour();
		 Integer mBandM = MBtime.getMinute();		
		 Integer mBandS = MBtime.getSecond();				 
		 Boolean isNewmBand = false;
		 //Log.info("pre mBcHECK-  " + " |mBandH " + mBandH + " |mBandM " + mBandM + " |mBandS " + mBandS);	 
		 
		 if(!this.mbhTimeArray.equals(null)) {
			 Integer mprevBandH = this.mbhTimeArray[0] ;		 
			 Integer mprevBandM = this.mbhTimeArray[1] ;		
			 Integer mprevBandS = this.mbhTimeArray[2] ;				 
			 //Log.info("setPrevMBtime |" + LocalTime.of(prevBandH, prevBandM, prevBandS).toString());		
			 
			 //Log.info("mBcHECK-         " + " | MBtime " + MBtime + " | prevTime " + LocalTime.of(mprevBandH, mprevBandM, mprevBandS));
			 if(MBtime.compareTo(LocalTime.of(mprevBandH, mprevBandM, mprevBandS)) != 0) {
				isNewmBand = true; 
			}else {
				isNewmBand = false;				
			}
			 //Log.info("isNewMBand-   |" + isNewmBand.toString() + " | mprevBandH " + mprevBandH + " | mprevBandM " + mprevBandM + " | mprevBandS " + mprevBandS);
		 }		 

		 this.mbhTimeArray[0] = mBandH;	
		 this.mbhTimeArray[1] = mBandM;		   
		 this.mbhTimeArray[2] = mBandS;	
		 //***************************************************//	 
		 //Log.info("post mBCheck- |" + isNewmBand.toString() + " | mbhTimeArray " + mbhTimeArray[0] + " | mbhTimeArray " + mbhTimeArray[1] + " | mbhTimeArray " + mbhTimeArray[2]);		
		 
		return isNewmBand;		
	 }	
	 
	 public Boolean setPrevMBLtime(LocalTime MBtime) {
		 //ArrayList<String> arlist = new ArrayList<String>( );
		 Integer mBandH = MBtime.getHour();
		 Integer mBandM = MBtime.getMinute();		
		 Integer mBandS = MBtime.getSecond();				 
		 Boolean isNewmBand = false;
		 //Log.info("pre mBcHECK-  " + " |mBandH " + mBandH + " |mBandM " + mBandM + " |mBandS " + mBandS);	 
		 
		 if(!this.mblTimeArray.equals(null)) {
			 Integer mprevBandH = this.mblTimeArray[0] ;		 
			 Integer mprevBandM = this.mblTimeArray[1] ;		
			 Integer mprevBandS = this.mblTimeArray[2] ;				 
			 //Log.info("setPrevMBtime |" + LocalTime.of(prevBandH, prevBandM, prevBandS).toString());		
			 
			 //Log.info("mBcHECK-         " + " | MBtime " + MBtime + " | prevTime " + LocalTime.of(mprevBandH, mprevBandM, mprevBandS));
			 if(MBtime.compareTo(LocalTime.of(mprevBandH, mprevBandM, mprevBandS)) != 0) {
				isNewmBand = true; 
			}else {
				isNewmBand = false;				
			}
			 //Log.info("isNewMBand-   |" + isNewmBand.toString() + " | mprevBandH " + mprevBandH + " | mprevBandM " + mprevBandM + " | mprevBandS " + mprevBandS);
		 }		 

		 this.mblTimeArray[0] = mBandH;	
		 this.mblTimeArray[1] = mBandM;		   
		 this.mblTimeArray[2] = mBandS;	
		 //***************************************************//	 
		 //Log.info("post mBCheck- |" + isNewmBand.toString() + " | mblTimeArray " + mblTimeArray[0] + " | mblTimeArray " + mblTimeArray[1] + " | mblTimeArray " + mblTimeArray[2]);		
		 
		return isNewmBand;		
	 }

     public void compiledata(String PName, String Plevel, String Starttime, String PTrend) throws Exception {	 
    	String plotStr = getStrings(PName);
    	LocalTime TimeS = getTimes(Starttime); 	
		//LocalDateTime TimeNow = localT(); 
		Boolean newBPlot = false; // change these back to null then check for null below and add conditions for plotting or not plotting?
		Boolean newMBPlot = false;
		
		if (!Plevel.contains("999999")) {
			this.Price = Double.parseDouble(Plevel);		
		}else {
			this.Price = Double.NaN;		
		}
	
		if(PTrend.equals("TRUE") || PTrend.equals("FALSE")) {		
			if(plotStr.equals("BHi") || plotStr.equals("BHi's") ){
				newBPlot = this.setPrevBHtime(TimeS) ;		
			}else if(plotStr.equals("BLo")|| plotStr.equals("BLo's") ) {
				newBPlot = this.setPrevBLtime(TimeS) ;		
			}	
		}else if(PTrend.equals("m_TRUE") || PTrend.equals("m_FALSE")) {
			if(plotStr.equals("mBHi") || plotStr.equals("mBHi's") ){
				newMBPlot = this.setPrevMBHtime(TimeS)  ;
			}else if(plotStr.equals("mBLo")|| plotStr.equals("mBLo's") ) {
				newMBPlot = this.setPrevMBLtime(TimeS) ;	
			}				
		}

		//Log.info("compileData-   " + " | plotStr " + plotStr + " | Price " + Price + " | Bool " + newBPlot + " | MBool= " + newMBPlot  );   		
				
    	if(newBPlot != null) {
  			//Log.info(plotStr + " |Price= " + Price + " |TimeS= " + TimeS + " |Bool= " + newBPlot); 					    		 
    		  if(PTrend.equals("TRUE")) {	    			  
    			  this.bearBandH.addPoint(Double.NaN);
  	    		  this.bearBandL.addPoint(Double.NaN);     			  
	  
	        		if ((newBPlot.equals(true) || this.Price.isNaN()) && (plotStr.equals("BHi") || plotStr.equals("BHi's")) )  { 	
	    			 	//Log.info("++++++++++++++++++ DONT PLOT    BandH |" + plotStr + " +++++++++++++++++");  	    		
	    	    		this.bullBandH.addPoint(Double.NaN);
	    	    		this.bullBandH.addPoint(this.Price/pipV);   	    		
	    	    	}else if (plotStr.equals("BHi") || plotStr.equals("BHi's")){
	    		 		//Log.info("++++++++++++++++++ OK to Plot  BandH | " + plotStr + " +++++++++++++++++");	
	    	    		//Log.info("OK to Plot           BandH |" + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    	    		this.bullBandH.addPoint(this.Price/pipV);   
	    		    }	
	    	   	
	    	    	if ((newBPlot.equals(true) || this.Price.isNaN())  && (plotStr.equals("BLo") || plotStr.equals("BLo's")) ) { 	    		   	    		   
	    	    		//Log.info("++++++++++++++++++ DONT PLOT BandL |" + plotStr + " +++++++++++++++++");  
	    	    		this.bullBandL.addPoint(Double.NaN);  
	    	    		this.bullBandL.addPoint(this.Price/pipV); 	    		
	    			}else if (plotStr.equals("BLo") || plotStr.equals("BLo's")){
	    		 		//Log.info("++++++++++++++++++ OK to Plot  BandL | " + plotStr + " +++++++++++++++++");	
	    	    		//Log.info("OK to Plot           BandL |" + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    	    		this.bullBandL.addPoint(this.Price/pipV);   		    		
	    		    }	   			   			  
    		  }else if(PTrend.equals("FALSE")) {
    			  this.bullBandH.addPoint(Double.NaN);
    			  this.bullBandL.addPoint(Double.NaN);  
    			  
	        		if ((newBPlot.equals(true) || this.Price.isNaN()) && (plotStr.equals("BHi") || plotStr.equals("BHi's")) )  { 	
	    			 	//Log.info("++++++++++++++++++ DONT PLOT    BandH |" + plotStr + " +++++++++++++++++");  	    		
	    	    		this.bearBandH.addPoint(Double.NaN);
	    	    		this.bearBandH.addPoint(this.Price/pipV);   	    		
	    	    	}else if (plotStr.equals("BHi") || plotStr.equals("BHi's")){
	    		 		//Log.info("++++++++++++++++++ OK to Plot  BandH | " + plotStr + " +++++++++++++++++");	
	    	    		//Log.info("OK to Plot           BandH |" + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    	    		this.bearBandH.addPoint(this.Price/pipV);   
	    		    }	
	        		
	    	    	if ((newBPlot.equals(true) || this.Price.isNaN())  && (plotStr.equals("BLo") || plotStr.equals("BLo's")) ) { 	    		   	    		   
	    	    		//Log.info("++++++++++++++++++ DONT PLOT BandL |" + plotStr + " +++++++++++++++++");  
	    	    		this.bearBandL.addPoint(Double.NaN);  
	    	    		this.bearBandL.addPoint(this.Price/pipV); 	    		
	    			}else if (plotStr.equals("BLo") || plotStr.equals("BLo's")){
	    		 		//Log.info("++++++++++++++++++ OK to Plot  BandL | " + plotStr + " +++++++++++++++++");	
	    	    		//Log.info("OK to Plot           BandL |" + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    	    		this.bearBandL.addPoint(this.Price/pipV);   		    		
	    		    }	   			   			  
    		  }
		}else {			
			throw new Exception("BTimes did not compile correctly and are null!");
		}
    	
		
		if(newMBPlot != null) {
			//Log.info(plotStr + " |Price= " + Price + " |TimeS= " + TimeS + " |Bool= " + newMBPlot); 					
  		      if(PTrend.equals("m_TRUE")) {	 		    	  
  		    	  this.MbearBandH.addPoint(Double.NaN);
  		    	  this.MbearBandL.addPoint(Double.NaN);		
  		    	  
					if ((newMBPlot.equals(true) || this.Price.isNaN()) && plotStr.equals("mBHi") ) { 	    		    		
			    		//Log.info("++++++++++++++++++ DONT PLOT     MBandH |" + plotStr + " +++++++++++++++++");  
			    		this.MbullBandH.addPoint(Double.NaN);
			    		this.MbullBandH.addPoint(this.Price/pipV);  	    		
				    }else if (plotStr.equals("mBHi")) {
				 		//Log.info("++++++++++++++++++ OK to Plot  MBandH | " + plotStr + " +++++++++++++++++");	
			    		//Log.info("OK to Plot          MBandH |" + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			    		this.MbullBandH.addPoint(this.Price/pipV);   
				    }	
			    	    	
			    	if ((newMBPlot.equals(true) || this.Price.isNaN()) && plotStr.equals("mBLo") ) { 	    		   	    		   
			    		//Log.info("++++++++++++++++++ DONT PLOT     MBandL |" + plotStr + " +++++++++++++++++");  
			    		this.MbullBandL.addPoint(Double.NaN);   	
			    		this.MbullBandL.addPoint(this.Price/pipV);  	    		    			    		
					}else if (plotStr.equals("mBLo")) {
				 		//Log.info("++++++++++++++++++ OK to Plot  MBandL | " + plotStr + " +++++++++++++++++");	
			    		//Log.info("OK to Plot          MBandL |" + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			    		this.MbullBandL.addPoint(this.Price/pipV);   	
				    }
			   } else if(PTrend.equals("m_FALSE")) {	   				   
				   this.MbullBandH.addPoint(Double.NaN);
				   this.MbullBandL.addPoint(Double.NaN);   
				   
					if ((newMBPlot.equals(true) || this.Price.isNaN()) && plotStr.equals("mBHi") ) { 	    		    		
			    		//Log.info("++++++++++++++++++ DONT PLOT     MBandH |" + plotStr + " +++++++++++++++++");  
			    		this.MbearBandH.addPoint(Double.NaN);
			    		this.MbearBandH.addPoint(this.Price/pipV);  	    		
				    }else if (plotStr.equals("mBHi")) {
				 		//Log.info("++++++++++++++++++ OK to Plot  MBandH | " + plotStr + " +++++++++++++++++");	
			    		//Log.info("OK to Plot          MBandH |" + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			    		this.MbearBandH.addPoint(this.Price/pipV);   
				    }	
			    	    	
			    	if ((newMBPlot.equals(true) || this.Price.isNaN()) && plotStr.equals("mBLo") ) { 	    		   	    		   
			    		//Log.info("++++++++++++++++++ DONT PLOT     MBandL |" + plotStr + " +++++++++++++++++");  
			    		this.MbearBandL.addPoint(Double.NaN);   	
			    		this.MbearBandL.addPoint(this.Price/pipV);  	    		    		
					}else if (plotStr.equals("mBLo")) {
				 		//Log.info("++++++++++++++++++ OK to Plot  MBandL | " + plotStr + " +++++++++++++++++");	
			    		//Log.info("OK to Plot          MBandL |" + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			    		this.MbearBandL.addPoint(this.Price/pipV);   	
				    }
			   }
				   
  		      
		}else {			
			throw new Exception("MBTimes did not compile correctly and are null!");
		}

		
	    //Log.info("added POINTS!");   	
	}
	 
	/* @Override
	 public void onBar(OrderBook orderBook, Bar bar) {
	  }   
	*/
	
     protected void onSettingsChange(Long UpdInt) {
		 	Log.info("onSettingsChange " + localT() + " |NewUpdateInterval= " + UpdInt.toString() + " seconds."); 
		 	
		 	//nCustomInterval = (long) defaultUpdateInterval * 1_000_000_000L;//seconds to nanos ?;
		 	//Log.infoFree(interval); 
    }

	    			
	 @Override
	 public StrategyPanel[] getCustomSettingsPanels() {
	     StrategyPanel p1 = getIntervalSettingsPanel();		 
	     StrategyPanel p2 = getBStyleSettingsPanel();
	     StrategyPanel p3 = getMBStyleSettingsPanel();	
	     return new StrategyPanel[] { p1, p2, p3};        
	 }
	 
	 private StrategyPanel getIntervalSettingsPanel() {
	     BookmapSettingsPanel panel = new BookmapSettingsPanel("Update Interval");
	     addIntervalSettings(panel);

	     return panel;
	 }	
	 
	 private StrategyPanel getBStyleSettingsPanel() {
	     BookmapSettingsPanel panel = new BookmapSettingsPanel("Band settings");
	     addBLineStyleSettings(panel);
	     addBLineWidthSettings(panel);     
	     addBColorsSettings(panel);
	     return panel;
	 }	 
	 
	private StrategyPanel getMBStyleSettingsPanel() {
		 BookmapSettingsPanel panel = new BookmapSettingsPanel("mBand settings");
		 addMBLineStyleSettings(panel);
		 addMBLineWidthSettings(panel);	 
		 addMBColorsSettings(panel);
		 return panel;
	}	  
	
	private void addIntervalSettings(final BookmapSettingsPanel panel) {
		   JComboBox<Integer> c = new JComboBox<>(new Integer[] { 5, 10, 15, 30, 45, 60});
		   setAlignment(c);
		   int selected = (int) (settings.cUpdateInt);
		   c.setSelectedItem(selected);
		   c.setEditable(false);
		   c.addActionListener(new ActionListener() {
		
		 // c.setSelectedItem(settings.lineWidth);
		 
		       @Override
		       public void actionPerformed(ActionEvent e) {                            	               
		           int newUpdateInt = (int) c.getSelectedItem();
		           if (newUpdateInt != settings.cUpdateInt) {
		        	   settings.cUpdateInt = newUpdateInt;	                          
		               nCustomInterval = newUpdateInt;  
		               onSettingsChange(nCustomInterval);      	                   
		           }
		       };
		   });	       
		   panel.addSettingsItem("Interval (seconds):", c);
		}
	
	 
	private void addBColorsSettings(final BookmapSettingsPanel panel) {   
	       panel.addSettingsItem("Bullish color:", createColorsConfigItem(true));		
	       panel.addSettingsItem("Bearish color:", createColorsConfigItem(false));
	}
	
	private void addMBColorsSettings(final BookmapSettingsPanel panel) {   
	      panel.addSettingsItem("Bullish color:", createMBColorsConfigItem(true));	
	      panel.addSettingsItem("Bearish color:", createMBColorsConfigItem(false));	        
	}	
	 	 
	private ColorsConfigItem createColorsConfigItem(boolean isBullish) {
	    Consumer<Color> c = new Consumer<Color>() {
	       @Override
	       public void accept(Color color) {    	    	   
	           if (isBullish) {
	               settings.colorBull = color;
	               bullBandH.setColor(settings.colorBull);
	               bullBandL.setColor(settings.colorBull);	               
	           } else{
	               settings.colorBear = color;
	               bearBandH.setColor(settings.colorBear);
	               bearBandL.setColor(settings.colorBear);	               
	           }   	
	       }	
	   };
	   
	    Color color = isBullish ? settings.colorBull : settings.colorBear;     
	    Color defaultColor = isBullish ? defBullc : defBearc;
		
	    //Log.info("TEST  " + x + " | " + color + " | " + defaultColor);
	    return new ColorsConfigItem(color, defaultColor, c);
		}
	
	 private ColorsConfigItem createMBColorsConfigItem(boolean isBullish) {
		    Consumer<Color> c = new Consumer<Color>() {
		       @Override
		       public void accept(Color color) {    	    	   
		           if (isBullish) {
		               settings.colorMBull = color;
		               MbullBandH.setColor(settings.colorMBull);
		               MbullBandL.setColor(settings.colorMBull);	               
		           } else{
		               settings.colorMBear = color;
		               MbearBandH.setColor(settings.colorMBear);
		               MbearBandL.setColor(settings.colorMBear);	               
		           }   	
		       }	
		   };
		 
		   Color color = isBullish ? settings.colorMBull : settings.colorMBear;     
		   Color defaultColor = isBullish ? defMBullc : defMBearc;
			
		   //Log.info("TEST  " + x + " | " + color + " | " + defaultColor);
		   return new ColorsConfigItem(color, defaultColor, c);
		}	
	
	
	 private void addBLineStyleSettings(final BookmapSettingsPanel panel) {
	     String[] lineStyles = Stream.of(LineStyle.values()).map(Object::toString).toArray(String[]::new);
	     JComboBox<String> c = new JComboBox<>(lineStyles);
	     c.setSelectedItem(settings.lineBStyle.toString());
	     c.setEditable(false);
	     c.addActionListener(new ActionListener() {
	         @Override
	         public void actionPerformed(ActionEvent e) {
	             int idx = c.getSelectedIndex();
	             if (idx != settings.lineBStyle.ordinal()) {
	                 settings.lineBStyle = LineStyle.values()[idx];
	                
	                 bullBandH.setLineStyle(settings.lineBStyle);	                            
	                 bullBandL.setLineStyle(settings.lineBStyle);	  
	                 bearBandH.setLineStyle(settings.lineBStyle);	                            
	                 bearBandL.setLineStyle(settings.lineBStyle);	
	             }
	         }
	     });		
	     panel.addSettingsItem("Band Line type:", c);
	  }   
	 
	 private void addMBLineStyleSettings(final BookmapSettingsPanel panel) {
	     String[] lineStyles = Stream.of(LineStyle.values()).map(Object::toString).toArray(String[]::new);
	     JComboBox<String> c = new JComboBox<>(lineStyles);
	     c.setSelectedItem(settings.lineMBStyle.toString());
	     c.setEditable(false);
	     c.addActionListener(new ActionListener() {
	         @Override
	         public void actionPerformed(ActionEvent e) {
	             int idx = c.getSelectedIndex();
	             if (idx != settings.lineMBStyle.ordinal()) {
	                 settings.lineMBStyle = LineStyle.values()[idx];
		             	
	                 MbullBandH.setLineStyle(settings.lineMBStyle);	                   
	                 MbullBandL.setLineStyle(settings.lineMBStyle);      
	                 MbearBandH.setLineStyle(settings.lineMBStyle);	                   
	                 MbearBandL.setLineStyle(settings.lineMBStyle);   	                 
	             }
	         }
	     });		
	     panel.addSettingsItem("mBand Line type:", c);
	}   

	 private void addBLineWidthSettings(final BookmapSettingsPanel panel) {
	      JComboBox<Integer> c = new JComboBox<>(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
	      setAlignment(c);
	      c.setSelectedItem(settings.lineBWidth);
	      c.setEditable(false);
	      c.addActionListener(new ActionListener() {
	
	          @Override
	          public void actionPerformed(ActionEvent e) {
	              int newLineWidth = (int) c.getSelectedItem();
	              if (newLineWidth != settings.lineBWidth) {
	                  settings.lineBWidth = newLineWidth;
	                  bullBandH.setWidth(settings.lineBWidth);    
	                  bullBandL.setWidth(settings.lineBWidth); 	                 	                                                
	                  bearBandH.setWidth(settings.lineBWidth);    
	                  bearBandL.setWidth(settings.lineBWidth); 	 	                  	                 	                                                
	              }
	          }
	      });
	      panel.addSettingsItem("Band Line width:", c);
	 }
	 
	 private void addMBLineWidthSettings(final BookmapSettingsPanel panel) {
	      JComboBox<Integer> c = new JComboBox<>(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
	      setAlignment(c);
	      c.setSelectedItem(settings.lineMBWidth);
	      c.setEditable(false);
	      c.addActionListener(new ActionListener() {

	          @Override
	          public void actionPerformed(ActionEvent e) {
	              int newLineWidth = (int) c.getSelectedItem();
	              if (newLineWidth != settings.lineMBWidth) {
	                  settings.lineMBWidth = newLineWidth;
	                  
	                  MbullBandH.setWidth(settings.lineMBWidth);     
	                  MbullBandL.setWidth(settings.lineMBWidth);   	                  
	                  MbearBandH.setWidth(settings.lineMBWidth);     
	                  MbearBandL.setWidth(settings.lineMBWidth);   	                                   	                	                                                
	              }
	          }
	      });
	      panel.addSettingsItem("mBand Line width:", c);
	  }
 
	  private void setAlignment(final JComboBox<?> c) {
	      ((JLabel)c.getRenderer()).setHorizontalAlignment(JLabel.LEFT);
	  }
	  	 
    @Override
    public void stop() {
         api.setSettings(settings);
 		 Log.info("Bye");        
    }    
}
    