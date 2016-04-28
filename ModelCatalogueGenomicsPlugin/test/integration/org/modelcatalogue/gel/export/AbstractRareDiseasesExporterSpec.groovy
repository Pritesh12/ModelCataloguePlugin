package org.modelcatalogue.gel.export

import grails.test.spock.IntegrationSpec
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.modelcatalogue.core.*
import org.modelcatalogue.core.api.ElementStatus
import org.modelcatalogue.core.audit.AuditService
import org.modelcatalogue.core.ddl.DataDefinitionLanguage
import org.modelcatalogue.core.publishing.DraftContext
import org.modelcatalogue.core.util.Metadata
import org.modelcatalogue.core.util.builder.ContextItem
import org.modelcatalogue.core.util.builder.DefaultCatalogueBuilder
import org.modelcatalogue.gel.RareDiseaseCsvExporter

/**
 * Created by rickrees on 10/03/2016.
 */
class AbstractRareDiseasesExporterSpec extends IntegrationSpec {

    AuditService auditService
    DataClassService dataClassService
    ElementService elementService
    DataModelService dataModelService
    InitCatalogueService initCatalogueService

    def setup() {
        initCatalogueService.initDefaultRelationshipTypes()
    }

    @Rule TemporaryFolder temporaryFolder

    String level2_id_1,level2_id_2
    String level3_id_1, level3_id_2, level3_id_3, level3_id_4
    String level4_id_1, level4_id_2, level4_id_3, level4_id_4
    String level5_id_1, level5_id_2, level5_id_3, level5_id_4
    String level6_inclusion_1,level6_inclusion_2,level6_inclusion_3,level6_inclusion_4
    String level6_exclusion_1,level6_exclusion_2,level6_exclusion_3,level6_exclusion_4
    String level6_priorGenetic_1,level6_priorGenetic_2,level6_priorGenetic_3,level6_priorGenetic_4
    String level6_prior_genes_1,level6_prior_genes_2,level6_prior_genes_3,level6_prior_genes_4

    // this model reflects the data mix of eligibility criteria, phenotypes & clinical tests that need to be extracted
    // by the two report generation methods it's a bit nasty looking but creates a fairly realistic model
    DataModel buildTestModel(boolean createPhenotypes) {
        DefaultCatalogueBuilder builder = new DefaultCatalogueBuilder(dataModelService, elementService)

        DataModel testModel = builder.build {
            dataModel(name: 'Test Data Model') {
                description "This is a data model for testing Eligibility OR Phenotype and Clinicals tests exports"

                dataClass (name: 'Dataclass Top Level 1 Root') {
                    for (int i in 1..2) {
                        dataClass name: "Disorder >>>$i<<< Level2", {
                            description "This is a description for Model $i"

                            for (int j in 1..2) {
                                dataClass name: "Disorder >>>$i<<< SubCondition Level3 Model Data Element $j", {
                                    description "This is a description for Model $i Data Element $j"

                                    dataClass name: "Disorder >>$i<< heading Level4 Model Data Element $j", {
                                        description "Disorder >>$i<< heading Level4 description for Model Data Element $j"

                                        dataClass name: "Disorder >$i< Eligibility Level5 Model $i Data Element $j", {
                                            description "Disorder >$i< heading Level5 description for Model $i Data Element $j"

                                            dataClass name: "Inclusion criteria name $i $j", {
                                                description "Inclusion criteria description  $i $j"
                                            }
                                            dataClass name: "Exclusion criteria name $i $j", {
                                                description "Exclusion criteria description  $i $j"
                                            }
                                            dataClass name: "Prior Genetic testing name $i $j", {
                                                description "Prior Genetic testing description  $i $j"
                                            }
                                            dataClass name: "Prior testing genes name $i $j", {
                                                description "Prior testing genes description  $i $j"
                                            }
                                            dataClass name: "Closing statement name $i $j", {
                                                description "Closing statement description  $i $j"
                                            }
                                            dataClass name: "Guidance name $i $j", {
                                                description "Guidance description  $i $j"
                                            }
                                        }

                                        dataClass name: "Disorder >$i< Phenotypes Level5 Model $i Data Element $j", {
                                            description "Disorder >$i< heading Level5 description for Model $i Data Element $j"

                                            if (createPhenotypes) {
                                                for (int k in 1..15) {
                                                    dataClass name: "Phenotype ($k) name $i $j", {
                                                        ext "OBO ID", "HP:" + (i + j + k)
                                                    }
                                                }
                                            }
                                        }

                                        dataClass name: "Disorder >$i< Clinical tests Level5 Model $i Data Element $j", {
                                            description "Disorder >$i< heading Level5 description for Model $i Data Element $j"

                                            for (int k in 1..5) {
                                                dataClass name: "Clinical tests ($k) name $i $j", {
                                                }
                                            }
                                        }

                                        dataClass name: "Disorder >$i< Guidance name $i $j", {
                                            description "Guidance description  $i $j"
                                        }
                                    }
                                }
                            }

                        }
                    }
                }

                ext Metadata.OWNER, 'The Owner'
                ext Metadata.ORGANISATION, 'The Organisation'
                ext Metadata.AUTHORS, 'Author One, Author Two, Author Three'
                ext Metadata.REVIEWERS, 'Reviewer One, Reviewer Two, Reviewer Three'

            }
        }

        return testModel

    }


    DataClass makeChanges(DataClass finalized) {
        println "status= $finalized.status"

        DataClass model = DataClass.findByNameAndStatus('Dataclass Top Level 1 Root', ElementStatus.DRAFT)

        DataDefinitionLanguage.with('Test Data Model') {

            update 'hierarchy' of 'Disorder >1< Phenotypes Level5 Model 1 Data Element 1' remove 'Phenotype (2) name 1 1'

            update 'name' of 'Phenotype (5) name 1 1' to 'Phenotype (5) changed name'   //PROPERTY_CHANGED

            update 'OBO ID' of 'Phenotype (6) name 1 1' to 'modified OBO ID'            //METADATA_UPDATED

            remove 'OBO ID' of 'Phenotype (7) name 1 1'            //METADATA_UPDATED  - cehck this again - nothing found for any of the change types

            create DataElement called 'New Phenotype DataElement'                       //NEW_ELEMENT_CREATED
            update 'containment' of 'Phenotype (8) name 1 1' add 'New Phenotype DataElement'    //RELATIONSHIP_CREATED - check, none found

            create DataElement called '2nd New Phenotype DataElement'                       //NEW_ELEMENT_CREATED
            update 'containment' of 'Phenotype (9) name 1 1' add '2nd New Phenotype DataElement', 'Min Occurs': 0, 'Max Occurs': 2    //RELATIONSHIP_CREATED
                                                                                                        //RELATIONSHIP_METADATA_CREATED * 2

            update 'Min Occurs' of 'Phenotype (9) name 1 1' to '1'              //METADATA_CREATED
            update 'Max Occurs' of 'Phenotype (9) name 1 1' to '3'

            update 'description' of 'Disorder >1< Guidance name 1 1' to 'new textual description replaces old'

            update 'description' of 'Clinical tests (5) name 1 2' to 'description for Clinical tests (5) name 1 2  has been changed'

            create DataClass called 'New Guidance class'
            update 'description' of 'New Guidance class' to 'brand new description'
            update 'hierarchy' of 'Disorder >>1<< heading Level4 Model Data Element 2' add 'New Guidance class'

            update 'description' of 'Phenotype (2) name 2 2' to 'description for Phenotype (2) name 2 2 has been changed also'

        }

        return model

    }

    //this is painful...why did I make the test data so big?
    def findDataIds() {
        level2_id_1 = DataClass.findByNameIlike("Disorder%1%Level2").combinedVersion
        level2_id_2 = DataClass.findByNameIlike("Disorder%2%Level2").combinedVersion

        level3_id_1 = DataClass.findByNameIlike("Disorder%1%Level3%1").combinedVersion
        level3_id_2 = DataClass.findByNameIlike("Disorder%1%Level3%2").combinedVersion
        level3_id_3 = DataClass.findByNameIlike("Disorder%2%Level3%1").combinedVersion
        level3_id_4 = DataClass.findByNameIlike("Disorder%2%Level3%2").combinedVersion

        level4_id_1 = DataClass.findByNameIlike("Disorder%1%Level4%1").combinedVersion
        level4_id_2 = DataClass.findByNameIlike("Disorder%1%Level4%2").combinedVersion
        level4_id_3 = DataClass.findByNameIlike("Disorder%2%Level4%1").combinedVersion
        level4_id_4 = DataClass.findByNameIlike("Disorder%2%Level4%2").combinedVersion

        level5_id_1 = DataClass.findByNameIlike("Disorder%1%Level5%1").combinedVersion
        level5_id_2 = DataClass.findByNameIlike("Disorder%1%Level5%2").combinedVersion
        level5_id_3 = DataClass.findByNameIlike("Disorder%2%Level5%1").combinedVersion
        level5_id_4 = DataClass.findByNameIlike("Disorder%2%Level5%2").combinedVersion

        level6_inclusion_1 = DataClass.findByNameIlike("Inclusion%1 1").combinedVersion
        level6_inclusion_2 = DataClass.findByNameIlike("Inclusion%1 2").combinedVersion
        level6_inclusion_3 = DataClass.findByNameIlike("Inclusion%2 1").combinedVersion
        level6_inclusion_4 = DataClass.findByNameIlike("Inclusion%2 2").combinedVersion

        level6_exclusion_1 = DataClass.findByNameIlike("Exclusion%1 1").combinedVersion
        level6_exclusion_2 = DataClass.findByNameIlike("Exclusion%1 2").combinedVersion
        level6_exclusion_3 = DataClass.findByNameIlike("Exclusion%2 1").combinedVersion
        level6_exclusion_4 = DataClass.findByNameIlike("Exclusion%2 2").combinedVersion

        level6_priorGenetic_1 = DataClass.findByNameIlike("Prior%Genetic%1 1").combinedVersion
        level6_priorGenetic_2 = DataClass.findByNameIlike("Prior%Genetic%1 2").combinedVersion
        level6_priorGenetic_3 = DataClass.findByNameIlike("Prior%Genetic%2 1").combinedVersion
        level6_priorGenetic_4 = DataClass.findByNameIlike("Prior%Genetic%2 2").combinedVersion

        level6_prior_genes_1 = DataClass.findByNameIlike("Prior%genes%1 1").combinedVersion
        level6_prior_genes_2 = DataClass.findByNameIlike("Prior%genes%1 2").combinedVersion
        level6_prior_genes_3 = DataClass.findByNameIlike("Prior%genes%2 1").combinedVersion
        level6_prior_genes_4 = DataClass.findByNameIlike("Prior%genes%2 2").combinedVersion

    }

    String getLevel2Id(def pos){
        return DataClass.findByNameIlike("Disorder%$pos%Level2").combinedVersion
    }

    String getLevel3Id(def level2pos, def level3pos){
        return DataClass.findByNameIlike("Disorder%$level2pos%Level3%$level3pos").combinedVersion
    }

    String getLevel4Id(def level2pos, def level3pos){
        return DataClass.findByNameIlike("Disorder%$level2pos%Level4%$level3pos").combinedVersion
    }

    String getLevel2Name(def pos){
        return "Disorder >>>$pos<<< Level2"
    }

    String getLevel3Name(def level2pos, def level3pos){
        return "Disorder >>>$level2pos<<< SubCondition Level3 Model Data Element $level3pos"
    }

    String getLevel4Name(def level2pos, def level3pos){
        return "Disorder >>$level2pos<< heading Level4 Model Data Element $level3pos"
    }

    String getClinicalTestId(def pos, def level2pos, def level3pos){
        return RareDiseaseCsvExporter.getVersionId(DataClass.findByNameIlike("Clinical tests ($pos) name $level2pos $level3pos"))
    }

}
