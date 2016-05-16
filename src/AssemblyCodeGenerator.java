import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

public class AssemblyCodeGenerator {
	final Type int_type   = IntType.getBuiltinType();
	final Type float_type = FloatType.getBuiltinType();
	final Type bool_type = BoolType.getBuiltinType();
	private int floatCounter;
	private int COutCounter;
	private int localCounter;
	private int globalCounter;
	
    // 1
    private int indent_level = 0;
    
    // 2
    private static final String ERROR_IO_CLOSE = 
        "Unable to close fileWriter";
    private static final String ERROR_IO_CONSTRUCT = 
        "Unable to construct FileWriter for file %s";
    private static final String ERROR_IO_WRITE = 
        "Unable to write to fileWriter";

    // 3
    private FileWriter fileWriter;
    
    // 4
    private static final String FILE_HEADER = 
        "/*\n" +
        " * Generated %s\n" + 
        " */\n\n";
        
    // 5
    public static final String SEPARATOR = "\t";
    
    // 6
    public static final String SET_OP = "set" + SEPARATOR;
    public static final String ADD_OP = "add" + SEPARATOR;
    public static final String STORE_OP = "st" + SEPARATOR;
    public static final String LOAD_OP = "ld" + SEPARATOR;
    public static final String TWO_PARAM = "%s" + SEPARATOR + "%s, %s\n";
    public static final String ONE_PARAM = "%s" + SEPARATOR + "%s\n";
    public static final String THREE_PARAM = "%s" + SEPARATOR + "%s, %s, %s\n";
    public static final String ALIGN = ".align" + SEPARATOR + SEPARATOR + "%s\n";
    public static final String GLOBAL = ".global" + SEPARATOR + SEPARATOR + "%s\n";
    public static final String SECTION = ".section" + SEPARATOR + "%s\n";
    public static final String SINGLE = ".single" + SEPARATOR + SEPARATOR + "%s\n";
    public static final String SKIP = ".skip" + SEPARATOR + SEPARATOR + "%s\n";
    public static final String WORD = ".word" + SEPARATOR + SEPARATOR + "%s\n";
    public static final String LOCALCOMMENT = "! %s = %s\n";
    public static final String LOCALCOMMENTCOUT = "! %s << %s\n";
    public static final String ASCIZ = ".asciz";
    public static final String STRINGFORMAT = ".$$.strFmt";
    
    public AssemblyCodeGenerator(String fileToWrite) {
        try {
            fileWriter = new FileWriter(fileToWrite);
            
            // 7
            writeAssembly(FILE_HEADER, (new Date()).toString());
        } catch (IOException e) {
            System.err.printf(ERROR_IO_CONSTRUCT, fileToWrite);
            e.printStackTrace();
            System.exit(1);
        }
    }
    

    // 8
    public void decreaseIndent() {
        indent_level--;
    }
    
    public void dispose() {
        try {
            fileWriter.close();
        } catch (IOException e) {
            System.err.println(ERROR_IO_CLOSE);
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void increaseIndent() {
        indent_level++;
    }
    

    
    // 9
    public void writeAssembly(String template, String ... params) {
        StringBuilder asStmt = new StringBuilder();
        
        // 10
        for (int i=0; i < indent_level; i++) {
            asStmt.append(SEPARATOR);
        }
        
        // 11
        asStmt.append(String.format(template, (Object[])params));
        
        try {
            fileWriter.write(asStmt.toString());
        } catch (IOException e) {
            System.err.println(ERROR_IO_WRITE);
            e.printStackTrace();
        }
    }
    
    public void doGlobalDecl(String initStatic, Type basicType, String id, STO initExpr)
    {
    	globalCounter++;
    	this.increaseIndent();
		if (initExpr != null){
			this.writeAssembly(this.SECTION, "\".data\"");
		}
		else {
			this.writeAssembly(this.SECTION, "\".bss\"");
		}
		this.writeAssembly(this.ALIGN, "4");
		if (initStatic != "static"){
			this.writeAssembly(this.GLOBAL, id);
		}
		this.decreaseIndent();
		this.writeAssembly(id + ":\n");
		this.increaseIndent();
		if (initExpr != null && initExpr.getType() == float_type){
			this.writeAssembly(this.SINGLE, "0r" + initExpr.getName() + "\n");
		}
		else if (initExpr != null && initExpr.getType() == int_type){
			this.writeAssembly(this.WORD, initExpr.getName());	
		}
		else if (initExpr != null && initExpr.getType() == bool_type && initExpr.getName() == "true"){
			this.writeAssembly(this.WORD, "1");	
		}
		else if (initExpr != null && initExpr.getType() == bool_type && initExpr.getName() == "false"){	
			this.writeAssembly(this.WORD, "0");	
		}
		else {
			this.writeAssembly(this.SKIP, "4\n");			
		}
		this.writeAssembly(this.SECTION, "\".text\"");
		this.writeAssembly(this.ALIGN, "4\n");		
		this.decreaseIndent();
    }
    
    public void doGlobalConstDecl(Type basicType, String id, STO initExpr)
    {
    	globalCounter++;
    	this.increaseIndent();
		if (initExpr != null){
			this.writeAssembly(this.SECTION, "\".data\"");
		}
		else {
			this.writeAssembly(this.SECTION, "\".bss\"");
		}
		this.writeAssembly(this.ALIGN, "4");
		this.decreaseIndent();
		this.writeAssembly(id + ":\n");
		this.increaseIndent();
		if (initExpr != null && initExpr.getType() == float_type){
			this.writeAssembly(this.SINGLE, "0r" + initExpr.getName() + "\n");
		}
		else if (initExpr != null && initExpr.getType() == int_type){
			this.writeAssembly(this.WORD, initExpr.getName());	
		}
		else if (initExpr != null && initExpr.getType() == bool_type && initExpr.getName() == "true"){
			this.writeAssembly(this.WORD, "1");	
		}
		else if (initExpr != null && initExpr.getType() == bool_type && initExpr.getName() == "false"){	
			this.writeAssembly(this.WORD, "0");	
		}
		else {
			this.writeAssembly(this.SKIP, "4\n");			
		}
		this.writeAssembly(this.SECTION, "\".text\"");
		this.writeAssembly(this.ALIGN, "4\n");		
		this.decreaseIndent();
    }
    
    public void doLocalDecl(String initStatic, Type basicType, String id, STO initExpr, int localCounter)
    {
		this.increaseIndent();	
		if (initExpr.getVisibility() ==  null && basicType == int_type){
			this.writeAssembly(this.LOCALCOMMENT, id, initExpr.getName());
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, Integer.toString(localCounter * -4), "%o1");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, "%fp", "%o1", "%o1");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, initExpr.getName(), "%o0");
			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");
		}
		else if (initExpr.getVisibility() !=  null && basicType == int_type){
    		this.writeAssembly("! " + id + " = " + initExpr.getName() + "\n");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, Integer.toString(localCounter * -4), "%o1");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, "%fp", "%o1", "%o1");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, initExpr.getOffset(), "%l7");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, initExpr.getBase(), "%l7", "%l7");
			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");			
			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");  
		}
		//else if (initExpr != null && basicType == float_type){
		// handles case where float local declaration with CONSTANT
		else if (initExpr.getVisibility() == null && basicType == float_type){
			floatCounter++;	
			this.writeAssembly(this.LOCALCOMMENT, id, initExpr.getName());
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, Integer.toString(localCounter * -4), "%o1");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, "%fp", "%o1", "%o1" + "\n");
			this.writeAssembly(this.SECTION, "\".rodata\"");
			this.writeAssembly(this.ALIGN, "4");	
			this.writeAssembly(".$$.float." + floatCounter + ":" + "\n");
			this.writeAssembly(this.SINGLE, "0r" + initExpr.getName() + "\n");
			this.writeAssembly(this.SECTION, "\".text\"");
			this.writeAssembly(this.ALIGN, "4");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, ".$$.float." + floatCounter, "%l7");
			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");
			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%f0", "[%o1]\n");			
		}
		// handles case where float local declaration with NON CONSTANT
		else if (initExpr.getVisibility() != null && basicType == float_type){
        	this.writeAssembly("! " + id + " = " + initExpr.getName() + "\n");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, Integer.toString(localCounter * -4), "%o1");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, "%fp", "%o1", "%o1");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, initExpr.getOffset(), "%l7\n");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, initExpr.getBase(), "%l7", "%l7");
			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");			
			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%f0", "[%o1]\n");
		}
		else if (initExpr.getVisibility() == null && basicType == bool_type){	
			this.writeAssembly(this.LOCALCOMMENT, id, initExpr.getName());
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, Integer.toString(localCounter * -4), "%o1");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, "%fp", "%o1", "%o1");
			if (initExpr.getName().equals("true")){
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, "1", "%o0");				
			}
			else {
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, "0", "%o0");									
			}
			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");
		}
		else if (initExpr.getVisibility() != null && basicType == bool_type){	
    		this.writeAssembly("! " + id + " = " + initExpr.getName() + "\n");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, Integer.toString(localCounter * -4), "%o1");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, "%fp", "%o1", "%o1");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, initExpr.getOffset(), "%l7");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, initExpr.getBase(), "%l7", "%l7");
			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");			
			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");  
		}
		this.decreaseIndent();	
    }
    
    public void doLocalConstDecl(Type basicType, String id, STO initExpr, int localCounter)
    {
		this.increaseIndent();	
		if (initExpr.getVisibility() ==  null && basicType == int_type){
			this.writeAssembly(this.LOCALCOMMENT, id, initExpr.getName());
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, Integer.toString(localCounter * -4), "%o1");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, "%fp", "%o1", "%o1");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, initExpr.getName(), "%o0");
			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");
		}
		else if (initExpr.getVisibility() !=  null && basicType == int_type){
    		this.writeAssembly("! " + id + " = " + initExpr.getName() + "\n");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, Integer.toString(localCounter * -4), "%o1");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, "%fp", "%o1", "%o1");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, initExpr.getOffset(), "%l7");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, initExpr.getBase(), "%l7", "%l7");
			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");			
			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");  
		}
		//else if (initExpr != null && basicType == float_type){
		// handles case where float local declaration with CONSTANT
		else if (initExpr.getVisibility() == null && basicType == float_type){
			floatCounter++;	
			this.writeAssembly(this.LOCALCOMMENT, id, initExpr.getName());
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, Integer.toString(localCounter * -4), "%o1");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, "%fp", "%o1", "%o1" + "\n");
			this.writeAssembly(this.SECTION, "\".rodata\"");
			this.writeAssembly(this.ALIGN, "4");	
			this.writeAssembly(".$$.float." + floatCounter + ":" + "\n");
			this.writeAssembly(this.SINGLE, "0r" + initExpr.getName() + "\n");
			this.writeAssembly(this.SECTION, "\".text\"");
			this.writeAssembly(this.ALIGN, "4");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, ".$$.float." + floatCounter, "%l7");
			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");
			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%f0", "[%o1]\n");			
		}
		// handles case where float local declaration with NON CONSTANT
		else if (initExpr.getVisibility() != null && basicType == float_type){
        	this.writeAssembly("! " + id + " = " + initExpr.getName() + "\n");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, Integer.toString(localCounter * -4), "%o1");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, "%fp", "%o1", "%o1");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, initExpr.getOffset(), "%l7\n");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, initExpr.getBase(), "%l7", "%l7");
			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");			
			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%f0", "[%o1]\n");
		}
		else if (initExpr.getVisibility() == null && basicType == bool_type){	
			this.writeAssembly(this.LOCALCOMMENT, id, initExpr.getName());
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, Integer.toString(localCounter * -4), "%o1");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, "%fp", "%o1", "%o1");
			if (initExpr.getName().equals("true")){
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, "1", "%o0");				
			}
			else {
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, "0", "%o0");									
			}
			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");
		}
		else if (initExpr.getVisibility() != null && basicType == bool_type){	
    		this.writeAssembly("! " + id + " = " + initExpr.getName() + "\n");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, Integer.toString(localCounter * -4), "%o1");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, "%fp", "%o1", "%o1");
			this.writeAssembly(this.TWO_PARAM, this.SET_OP, initExpr.getOffset(), "%l7");
			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, initExpr.getBase(), "%l7", "%l7");
			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");			
			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");  
		}
		this.decreaseIndent();	
    }
    
    public void doAssign(STO stoDes, STO expr, int assignCounter)
    {
    	this.increaseIndent();
     	if (stoDes.getVisibility().equals("Global") && expr.getVisibility() == null){
     		if (stoDes.getType() == int_type){
    			this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getName(), "%o0");
    			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");
     		}
     		else if (stoDes.getType() == bool_type){
    			this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
    			if (expr.getName().equals("true")){
    				this.writeAssembly(this.TWO_PARAM, this.SET_OP, "1", "%o0");				
    			}
    			else {
    				this.writeAssembly(this.TWO_PARAM, this.SET_OP, "0", "%o0");									
    			}
    			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");
     		}
     		else if (stoDes.getType() == float_type){
	    		floatCounter++;
				this.writeAssembly(this.LOCALCOMMENT, stoDes.getName(), expr.getName());
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1" + "\n");
				this.writeAssembly(this.SECTION, "\".rodata\"");
				this.writeAssembly(this.ALIGN, "4");	
				this.writeAssembly(".$$.float." + floatCounter + ":" + "\n");
				this.writeAssembly(this.SINGLE, "0r" + expr.getName() + "\n");
				this.writeAssembly(this.SECTION, "\".text\"");
				this.writeAssembly(this.ALIGN, "4");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, ".$$.float." + floatCounter, "%l7");
				this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");
				this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%f0", "[%o1]\n");
    		}
			
    	}
     	else if (stoDes.getVisibility().equals("Local") && expr.getVisibility() == null){
     		if (stoDes.getType() == int_type){
    			this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getName(), "%o0");
    			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");
     		}
     		else if (stoDes.getType() == bool_type){
    			this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
    			if (expr.getName().equals("true")){
    				this.writeAssembly(this.TWO_PARAM, this.SET_OP, "1", "%o0");				
    			}
    			else {
    				this.writeAssembly(this.TWO_PARAM, this.SET_OP, "0", "%o0");									
    			}
    			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");
     		}
     		else if (stoDes.getType() == float_type){
	    		floatCounter++;
				this.writeAssembly(this.LOCALCOMMENT, stoDes.getName(), expr.getName());
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1" + "\n");
				this.writeAssembly(this.SECTION, "\".rodata\"");
				this.writeAssembly(this.ALIGN, "4");	
				this.writeAssembly(".$$.float." + floatCounter + ":" + "\n");
				this.writeAssembly(this.SINGLE, "0r" + expr.getName() + "\n");
				this.writeAssembly(this.SECTION, "\".text\"");
				this.writeAssembly(this.ALIGN, "4");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, ".$$.float." + floatCounter, "%l7");
				this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");
				this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%f0", "[%o1]\n");
     		}
     	}
     	else if (stoDes.getVisibility().equals("Global") && expr.getVisibility().equals("Global")){
     		if (stoDes.getType() == int_type){
        		this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getOffset(), "%l7");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, expr.getBase(), "%l7", "%l7");
    			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");			
    			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");     			
     		}
     		else if (stoDes.getType() == bool_type){
        		this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getOffset(), "%l7");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, expr.getBase(), "%l7", "%l7");
    			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");			
    			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");    
     		}
     		else if (stoDes.getType() == float_type){
	    		this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getOffset(), "%l7");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, expr.getBase(), "%l7", "%l7");
				this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");			
				this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%f0", "[%o1]\n");
    		}
    	}
    	else if (stoDes.getVisibility().equals("Global") && expr.getVisibility().equals("Local")){
     		if (stoDes.getType() == int_type){
        		this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getOffset(), "%l7");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, expr.getBase(), "%l7", "%l7");
    			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");			
    			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");  
     		}
     		else if (stoDes.getType() == bool_type){
	    		this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getOffset(), "%l7");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, expr.getBase(), "%l7", "%l7");
				this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");			
				this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");
     		}
     		else if (stoDes.getType() == float_type){
	        	this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getOffset(), "%l7");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, expr.getBase(), "%l7", "%l7");
				this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");			
				this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%f0", "[%o1]\n");
    		}
    	}
    	else if (stoDes.getVisibility().equals("Local") && expr.getVisibility().equals("Global")){
     		if (stoDes.getType() == int_type){
        		this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getOffset(), "%l7");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, expr.getBase(), "%l7", "%l7");
    			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");			
    			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");  
     		}
     		else if (stoDes.getType() == bool_type){
	    		this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getOffset(), "%l7");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, expr.getBase(), "%l7", "%l7");
				this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");			
				this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");
     		}
     		else if (stoDes.getType() == float_type){
	            this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getOffset(), "%l7");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, expr.getBase(), "%l7", "%l7");
				this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");			
				this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%f0", "[%o1]\n");	
    		}
    	}
    	else if (stoDes.getVisibility().equals("Local") && expr.getVisibility().equals("Local")){
     		if (stoDes.getType() == int_type){
        		this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
    			this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getOffset(), "%l7");
    			this.writeAssembly(this.THREE_PARAM, this.ADD_OP, expr.getBase(), "%l7", "%l7");
    			this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");			
    			this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");  
     		}
     		else if (stoDes.getType() == bool_type){
	    		this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getOffset(), "%l7");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, expr.getBase(), "%l7", "%l7");
				this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");			
				this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%o0", "[%o1]\n");
     		}
     		else if (stoDes.getType() == float_type){
	            this.writeAssembly("! " + stoDes.getName() + " = " + expr.getName() + "\n");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, stoDes.getOffset(), "%o1");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, stoDes.getBase(), "%o1", "%o1");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, expr.getOffset(), "%l7");
				this.writeAssembly(this.THREE_PARAM, this.ADD_OP, expr.getBase(), "%l7", "%l7");
				this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");			
				this.writeAssembly(this.TWO_PARAM, this.STORE_OP, "%f0", "[%o1]\n");	
    		}
    	}
    	else {
    		
    	}
     	this.decreaseIndent();
    }
    
    void COutAssembly(Vector<STO> exprList){
    	for (int i = 0; i < exprList.size(); i++){
			if (exprList.get(i) == null){
				this.increaseIndent();
				this.writeAssembly("! cout << " +"endl" + "\n");
				this.writeAssembly(this.TWO_PARAM, this.SET_OP, ".$$.strEndl", "%o0");
				this.writeAssembly(this.ONE_PARAM, "call", "printf");
				this.writeAssembly("nop\n\n");
				this.decreaseIndent();
			}
			else {
				this.increaseIndent();
				System.out.println(exprList.get(i));
				// expression
				if (exprList.get(i).getType() == int_type && exprList.get(i) instanceof ExprSTO){
					//this.doAssign()
				}
				// variable const
				else if (exprList.get(i).getType() == int_type && exprList.get(i) instanceof VarSTO){
					this.writeAssembly("! cout " + exprList.get(i).getName() + "\n");
					this.writeAssembly(this.TWO_PARAM, this.SET_OP, exprList.get(i).getOffset(), "%l7");
					this.writeAssembly(this.THREE_PARAM, this.ADD_OP, exprList.get(i).getBase(), "%l7", "%l7");
					this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o1");
					this.writeAssembly(this.TWO_PARAM, this.SET_OP, ".$$.intFmt", "%o0");
					this.writeAssembly(this.ONE_PARAM, "call", "printf");
					this.writeAssembly("nop\n\n");
				}
				// literal const
				else if (exprList.get(i).getType() == int_type && exprList.get(i) instanceof ConstSTO){
					this.writeAssembly("! cout " + exprList.get(i).getName());
					this.writeAssembly(this.TWO_PARAM, this.SET_OP, ((ConstSTO)exprList.get(i)).getValue().toString(), "%o1");
					this.writeAssembly(this.TWO_PARAM, this.SET_OP, ".$$.intFmt", "%o0");
					this.writeAssembly(this.ONE_PARAM, "call", "printf");
					this.writeAssembly("nop\n\n");
				}
				else if (exprList.get(i).getType() == float_type && exprList.get(i) instanceof ExprSTO){
					this.writeAssembly("! cout << " + exprList.get(i).getName() + "\n");
					this.writeAssembly(this.TWO_PARAM, this.SET_OP, exprList.get(i).getOffset(), "%l7");
					this.writeAssembly(this.THREE_PARAM, this.ADD_OP, exprList.get(i).getBase(), "%l7", "%l7");
					this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");
					this.writeAssembly(this.ONE_PARAM, "call", "printfloat");
					this.writeAssembly("nop\n\n");				
				}
				else if (exprList.get(i).getType() == float_type && exprList.get(i) instanceof VarSTO){
					this.writeAssembly("! cout << " + exprList.get(i).getName() + "\n");
					this.writeAssembly(this.TWO_PARAM, this.SET_OP, exprList.get(i).getOffset(), "%l7");
					this.writeAssembly(this.THREE_PARAM, this.ADD_OP, exprList.get(i).getBase(), "%l7", "%l7");
					this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");
					this.writeAssembly(this.ONE_PARAM, "call", "printfloat");
					this.writeAssembly("nop\n\n");				
				}
				else if (exprList.get(i).getType() == float_type && exprList.get(i) instanceof ConstSTO){
					this.writeAssembly("! cout << " + exprList.get(i).getName() + "\n");
					floatCounter++;
					this.writeAssembly("\n");
					this.writeAssembly(this.SECTION, "\".rodata\"");
					this.writeAssembly(this.ALIGN, "4");	
					this.writeAssembly(".$$.float." + floatCounter + ":" + "\n");
					this.writeAssembly(this.SINGLE, "0r" + exprList.get(i).getName() + "\n");
					this.writeAssembly(this.SECTION, "\".text\"");
					this.writeAssembly(this.ALIGN, "4");
					this.writeAssembly(this.TWO_PARAM, this.SET_OP, ".$$.float." + floatCounter, "%l7");
					this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%f0");
					this.writeAssembly(this.ONE_PARAM, "call", "printfloat");
					this.writeAssembly("nop\n\n");
					
				}
				else if (exprList.get(i).getType() == bool_type && exprList.get(i) instanceof ExprSTO){
					this.writeAssembly("! cout << " + exprList.get(i).getName() + "\n");
					this.writeAssembly(this.TWO_PARAM, this.SET_OP, exprList.get(i).getOffset(), "%l7");
					this.writeAssembly(this.THREE_PARAM, this.ADD_OP, exprList.get(i).getBase(), "%l7", "%l7");
					this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");
					this.writeAssembly(this.ONE_PARAM, "call", ".$$.printBool");
					this.writeAssembly("nop\n\n");				
				}
				else if (exprList.get(i).getType() == bool_type && exprList.get(i) instanceof VarSTO){
					this.writeAssembly("! cout << " + exprList.get(i).getName() + "\n");
					this.writeAssembly(this.TWO_PARAM, this.SET_OP, exprList.get(i).getOffset(), "%l7");
					this.writeAssembly(this.THREE_PARAM, this.ADD_OP, exprList.get(i).getBase(), "%l7", "%l7");
					this.writeAssembly(this.TWO_PARAM, this.LOAD_OP, "[%l7]", "%o0");
					this.writeAssembly(this.ONE_PARAM, "call", ".$$.printBool");
					this.writeAssembly("nop\n\n");	
				}
				else if (exprList.get(i).getType() == bool_type && exprList.get(i) instanceof ConstSTO){
					this.writeAssembly("! cout << " + exprList.get(i).getName() + "\n");
					this.writeAssembly(this.TWO_PARAM, this.SET_OP, ((ConstSTO)exprList.get(i)).getValue().toString(), "%o0");
					this.writeAssembly(this.ONE_PARAM, "call", ".$$.printBool");
					this.writeAssembly("nop\n\n");	
				}
				else {
					++COutCounter;
					this.writeAssembly(this.SECTION, "\".rodata\"");
					this.writeAssembly(this.ALIGN, "4");
					this.writeAssembly(".$$.str." + COutCounter + ":\n");
					this.writeAssembly(this.ONE_PARAM, this.ASCIZ,"\""+ exprList.get(i).getName()+"\"" + "\n");
					this.writeAssembly(this.SECTION, "\".text\"");
					this.writeAssembly(this.ALIGN, "4");
					this.writeAssembly("! cout << " +  "\""+ exprList.get(i).getName()+"\"" + "\n");
					this.writeAssembly(this.TWO_PARAM, this.SET_OP,  this.STRINGFORMAT, "%o0");
					this.writeAssembly(this.TWO_PARAM, this.SET_OP, ".$$.str." + COutCounter, "%o1");
					this.writeAssembly(this.ONE_PARAM, "call", "printf");
					this.writeAssembly("nop\n\n");				
				}
				this.decreaseIndent();
			}

		}
    }
    
    // 12
    public static void main(String args[]) {
        AssemblyCodeGenerator myAsWriter = new AssemblyCodeGenerator("rc.s");

        myAsWriter.increaseIndent();
        myAsWriter.writeAssembly(TWO_PARAM, SET_OP, String.valueOf(4095), "%l0");
        myAsWriter.increaseIndent();
        myAsWriter.writeAssembly(TWO_PARAM, SET_OP, String.valueOf(1024), "%l1");
        myAsWriter.decreaseIndent();
        
        myAsWriter.writeAssembly(TWO_PARAM, SET_OP, String.valueOf(512), "%l2");
        
        myAsWriter.decreaseIndent();
        myAsWriter.dispose();
        
    }
    // sizeof and plus are fucking up for COUT
    //pass in left,right side of exprSTO

}