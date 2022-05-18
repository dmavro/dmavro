package IB;

//import com.opencsv.bean.CsvBindByPosition;

public class PlotVals {
	 private String Symbol;
	 private String PLevel;
	 private String PName;
	 private String StartTime;	 
	 private String PTrend;		 
	 
	public String getSymbol() {
		return Symbol;
	}
	public void setSymbol(String symbol) {
		Symbol = symbol;
	}
	public String getPLevel() {
		return PLevel;
	}
	public void setPLevel(String pLevel) {
		PLevel = pLevel;
	}
	public String getPName() {
		return PName;
	}
	public void setPName(String pName) {
		PName = pName;
	}
	public String getStartTime() {
		return StartTime;
	}
	public void setStartTime(String startTime) {
		StartTime = startTime;
	}
	public String getPTrend() {
		return PTrend;
	}
	public void setPTrend(String pTrend) {
		PTrend = pTrend;
	}
	
	
	@Override
  public String toString() 
  { 
      return "PlotVals [Symbol = " + Symbol + ", PLevel = " + PLevel + ", PName = " + PName + ", StartTime = " + StartTime 
                   + ", PTrend = " + PTrend + "]"; 
  } 

}