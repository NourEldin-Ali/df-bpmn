package it.unibz.deltabpmn.verification.mcmt.translation;

import it.unibz.deltabpmn.datalogic.ConjunctiveSelectQuery;
import it.unibz.deltabpmn.datalogic.EevarManager;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.core.SystemSorts;
import it.unibz.deltabpmn.exception.*;
import it.unibz.deltabpmn.processschema.blocks.Block;
import it.unibz.deltabpmn.processschema.blocks.ProcessBlock;
import it.unibz.deltabpmn.processschema.blocks.Task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public final class DABProcessTranslator {
	private ProcessBlock root;
	private String total_mcmt = "";
	private ConjunctiveSelectQuery safety_formula;
	private PrintWriter pw;
	private final DataSchema dataSchema;
	private DataSchemaTranslator dataSchemaTranslator;

	public DABProcessTranslator(String name, ProcessBlock block, DataSchema dataSchema, String exportPath) {
		this.root = block;
		this.root.getLifeCycleVariable().setLifeCycle(2);
		this.dataSchema = dataSchema;
		// create folder
		String path;
		if (exportPath == null) {
			path = System.getProperty("user.dir") + "/bpmn-model-translations";
		} else {
			path = exportPath;
		}
		File theDir = new File(path);
		if (!theDir.exists()) {
			theDir.mkdirs();
		}
		try {
			this.pw = new PrintWriter(new File(path + "/" + name + "_MCMT-Translation.txt")) {
				@Override
				public void println() {
					write('\n');
				}
			};
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		this.dataSchemaTranslator = DataSchemaTranslator.getInstance(this.dataSchema);

	}

//    public ProcessBlock getRoot() {
//        return root;
//    }
//
//    public void setRoot(ProcessBlock root) {
//        this.root = root;
//    }

//    public String getTotal_mcmt() {
//        return total_mcmt;
//    }
//
//    public void setTotal_mcmt(String total_mcmt) {
//        this.total_mcmt = total_mcmt;
//    }

//    public ConjunctiveSelectQuery getSafetyFormula() {
//        return safety_formula;
//    }

	public void setSafetyFormula(ConjunctiveSelectQuery safety_formula) {
		this.safety_formula = safety_formula;
	}

	// method for generating the total mcmt translation of the process
	private String processMCMTGeneration() throws InvalidInputException, UnmatchingSortException,
			EevarOverflowException, EmptyGuardException, UninitializedLifecyleVariableException {
		processMCMTGeneration(this.root);
		return this.total_mcmt;
	}

	private void processMCMTGeneration(Block block) throws InvalidInputException, UnmatchingSortException,
			EevarOverflowException, EmptyGuardException, UninitializedLifecyleVariableException {
		if (block instanceof Task) {
			this.total_mcmt += block.getMCMTTranslation();
			return;
		}

		// translation of the block, then recursively go through the depth of the tree
		System.out.println(block.getName());
		this.total_mcmt += block.getMCMTTranslation();
		for (Block sub_block : block.getSubBlocks()) {
			processMCMTGeneration(sub_block);
		}
	}

	private void generateDeclarationStatements() {
		// special treatment for integers
		this.dataSchema.removeSort(SystemSorts.INT);

		pw.println(":max_transitions_number 500");
		pw.println(":index int");
		pw.println(this.dataSchemaTranslator.Sorts().getAllElementDefinitions());
		pw.println(this.dataSchemaTranslator.CatalogRelations().getAllElementDeclarations());
		pw.println(this.dataSchemaTranslator.Constants().getAllElementDefinitions());
		pw.println(":db_driven ENTRY");// add db_driven keyword before the db_sort declarations
		pw.println(this.dataSchemaTranslator.Sorts().getAllElementDeclarations());
		pw.println(this.dataSchemaTranslator.CatalogRelations().getFunctionalCatalogView());
		pw.println(this.dataSchemaTranslator.Constants().getAllElementDeclarations());
		pw.println(this.dataSchemaTranslator.RepositoryRelations().getAllElementDeclarations());
		pw.println(this.dataSchemaTranslator.CaseVariables().getAllElementDeclarations());

	}

	private void generateInitializationStatements() {
		pw.print(":initial\n:var x\n:cnj ");
		pw.println(this.dataSchemaTranslator.RepositoryRelations().getInitializationDeclaration() + " "
				+ this.dataSchemaTranslator.CaseVariables().getInitializationDeclaration() + "");
	}

	public void generateMCMTTranslation() throws InvalidInputException, UnmatchingSortException, EevarOverflowException,
			EmptyGuardException, UninitializedLifecyleVariableException {
		generateDeclarationStatements();
		generateInitializationStatements();
		pw.println(":u_cnj " + this.safety_formula.getMCMTTranslation() + "");
		pw.print(EevarManager.generateEevarDeclarations());
		pw.println(this.processMCMTGeneration());
		pw.close();
	}

}