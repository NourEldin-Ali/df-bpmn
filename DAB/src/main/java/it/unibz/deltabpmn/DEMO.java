package it.unibz.deltabpmn;


import it.unibz.deltabpmn.bpmn.DfbpmnModelReader;
import it.unibz.deltabpmn.datalogic.*;
import it.unibz.deltabpmn.dataschema.core.DataSchema;
import it.unibz.deltabpmn.dataschema.core.SystemConstants;
import it.unibz.deltabpmn.dataschema.core.SystemSorts;
import it.unibz.deltabpmn.dataschema.elements.*;
import it.unibz.deltabpmn.exception.*;
import it.unibz.deltabpmn.processschema.blocks.ProcessBlock;
import it.unibz.deltabpmn.processschema.blocks.SequenceBlock;
import it.unibz.deltabpmn.processschema.blocks.Task;
import it.unibz.deltabpmn.processschema.core.ProcessSchema;
import it.unibz.deltabpmn.processschema.core.State;
import it.unibz.deltabpmn.verification.mcmt.translation.DABProcessTranslator;

import java.util.Scanner;

public class DEMO {

    public static void main(String[] args) throws Exception {
//        Scanner sc = new Scanner(System.in);
//        System.out.println("Insert your BPMN model path: ");
//        String path = sc.nextLine().trim();
//        System.out.println("Processing!");
    	String path = "C:\\Users\\AliNourEldin\\Desktop\\da-bpmn\\open-bpmn\\open-bpmn.glsp-client\\workspace\\delta_bpmn_final.bpmn";
        
        DfbpmnModelReader modelReader = new DfbpmnModelReader(path,null);

        for (DABProcessTranslator processTranslator : modelReader.getProcessTranslators())
            processTranslator.generateMCMTTranslation();
    

        
        // /mnt/c/Users/AliNourEldin/Desktop/da-bpmn/open-bpmn/DAB/bpmn-model-translations/Mortgage_MCMT-Translation.txt
        //to run mcmt:
        // cd /mnt/c/Users/AliNourEldin/Downloads/MCMT_distrib_v3.0Y/MCMT_distrib_v3.0Y/linux64/bin
        // ./bin/mcmt /mnt/c/Users/AliNourEldin/Desktop/da-bpmn/open-bpmn/DAB/bpmn-model-translations/Mortgage_MCMT-Translation.txt

    }
    


    /**
     * This is an example program demonstrating how to create DAB processes
     */
    private static void exampleDAB(DataSchema dataSchema) throws UnmatchingSortException, InvalidInputException, EevarOverflowException, RepoRelationOverflowException, EmptyGuardException, UninitializedLifecyleVariableException {
        // sorts
        Sort job_id = dataSchema.newSort("jobcatID");
        Sort user_id = dataSchema.newSort("userID");
        Sort num_age = dataSchema.newSort("NumAge");
        Sort int_sort = dataSchema.newSort("NumScore");


        //catalog relations
        CatalogRelation jobCategory = dataSchema.newCatalogRelation("Job_Category");
        jobCategory.addAttribute("jcid", job_id);

        CatalogRelation user = dataSchema.newCatalogRelation("user");
        Attribute u_id = user.addAttribute("uid", user_id);
        Attribute name = user.addAttribute("name", SystemSorts.STRING);
        Attribute age = user.addAttribute("age", num_age);


        //repository relations
        RepositoryRelation application = dataSchema.newRepositoryRelation("Application");
        Attribute jcid_app = application.addAttribute("jcidApp", job_id);
        Attribute uid_app = application.addAttribute("uidApp", user_id);
        Attribute score = application.addAttribute("score", int_sort);
        Attribute eligible = application.addAttribute("eligible", SystemSorts.BOOL);


        //case variables
        CaseVariable jcid = dataSchema.newCaseVariable("jcid", job_id, true);
        CaseVariable uid = dataSchema.newCaseVariable("uid", user_id, true);
        CaseVariable qualif = dataSchema.newCaseVariable("qualif", SystemSorts.BOOL, true);
        CaseVariable result = dataSchema.newCaseVariable("result", SystemSorts.BOOL, true);
        CaseVariable winner = dataSchema.newCaseVariable("winner", user_id, true);


        // some transitions
        ConjunctiveSelectQuery ins_job_cat_guard = new ConjunctiveSelectQuery(dataSchema, jobCategory.getAttributeByIndex(0));
        InsertTransition ins_job_cat = new InsertTransition("InsertJobCategory", ins_job_cat_guard, dataSchema);
        ins_job_cat.setControlCaseVariableValue(jcid, dataSchema.newConstant("jcid", job_id));


        ConjunctiveSelectQuery ins_user_guard = new ConjunctiveSelectQuery(dataSchema, user.getAttributeByIndex(0));
        InsertTransition ins_user = new InsertTransition("InsertUser", ins_user_guard, dataSchema);
        ins_user.setControlCaseVariableValue(uid, dataSchema.newConstant("uid", user_id));


        InsertTransition check_qual = new InsertTransition("CheckQual", dataSchema);
        check_qual.setControlCaseVariableValue(qualif, SystemConstants.TRUE);


        BulkUpdate markE = new BulkUpdate("MarkE", new ConjunctiveSelectQuery(dataSchema), application, dataSchema);
        markE.root.addGreaterThanCondition(application.getAttributeByIndex(2), 80)
                .addLessThanCondition(application.getAttributeByIndex(2), 100);
        markE.root.addTrueChild().updateAttributeValue(application.getAttributeByIndex(3), "True");
        markE.root.addFalseChild().updateAttributeValue(application.getAttributeByIndex(3), "False");


        // some transitions
        ConjunctiveSelectQuery sel_winner_guard = new ConjunctiveSelectQuery(dataSchema, application.getAttributeByIndex(0),
                application.getAttributeByIndex(1), application.getAttributeByIndex(2), application.getAttributeByIndex(3));
        sel_winner_guard.addBinaryCondition(BinaryConditionProvider.equality(application.getAttributeByIndex(3), SystemConstants.TRUE));
        DeleteTransition sel_winner = new DeleteTransition("Sel_Winner", sel_winner_guard, dataSchema);
        sel_winner.delete(application, jcid_app, uid_app, score, eligible);
        sel_winner.setControlCaseVariableValue(jcid, dataSchema.newConstant("jcid_app", job_id));
        sel_winner.setControlCaseVariableValue(uid, dataSchema.newConstant("uid_app", user_id));
        sel_winner.setControlCaseVariableValue(winner, dataSchema.newConstant("uid_app", user_id));
        sel_winner.setControlCaseVariableValue(result, dataSchema.newConstant("eligible", SystemSorts.BOOL));
        sel_winner.setControlCaseVariableValue(qualif, SystemConstants.FALSE);


        ConjunctiveSelectQuery prova = new ConjunctiveSelectQuery(dataSchema, jobCategory.getAttributeByIndex(0));
        prova.addBinaryCondition(BinaryConditionProvider.equality(jobCategory.getAttributeByIndex(0), dataSchema.newConstant("HR", job_id)));


        //example
        ProcessSchema processSchema = new ProcessSchema(dataSchema);
        Task decideEligible = processSchema.newTask("decide_eligible", markE);
        Task selectWinner = processSchema.newTask("select_winner", sel_winner);


        SequenceBlock seq = processSchema.newSequenceBlock("sequence_block");
        seq.addFirstBlock(decideEligible);
        seq.addSecondBlock(selectWinner);

        ProcessBlock root = processSchema.newProcessBlock("root_process");
        root.addBlock(seq);

        DABProcessTranslator assign_job_process = new DABProcessTranslator("job_hiring_process", root, dataSchema,null);

        //this property can be assigned to a list of properties to check in the BPMN model! the properties get parsed and (they're just conjunctive queries! we have a parser for conjunctive queries!)
        //run MCMT on the generated files
        ConjunctiveSelectQuery safety_property = new ConjunctiveSelectQuery(dataSchema);
        safety_property.addBinaryCondition(BinaryConditionProvider.equality(root.getLifeCycleVariable(), State.ENABLED));
        safety_property.addBinaryCondition(BinaryConditionProvider.equality(winner, SystemConstants.NULL));

        assign_job_process.setSafetyFormula(safety_property);

        assign_job_process.generateMCMTTranslation();

    }

}


