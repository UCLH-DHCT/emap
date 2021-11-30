package uk.ac.ucl.rits.inform.datasources.ids.hl7;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.Version;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.CustomModelClassFactory;

/**
 * Allows custom HL7 models to be used, while also setting a canonical HL7 version.
 */
public class CustomModelWithDefaultVersion extends CustomModelClassFactory {
    private String customVersion;

    /**
     * Create custom model factory with default hl7 version.
     * @param customPackageForModels package reference for custom models
     * @param canonicalVersion       default version for hl7 parsing
     */
    public CustomModelWithDefaultVersion(String customPackageForModels, String canonicalVersion) {
        super(customPackageForModels);
        if (canonicalVersion == null || !Version.supportsVersion(canonicalVersion)) {
            throw new IllegalArgumentException("Unknown version: " + canonicalVersion);
        }
        this.customVersion = canonicalVersion;
    }

    @Override
    public Class<? extends Message> getMessageClass(String name, String version, boolean isExplicit) throws HL7Exception {
        return super.getMessageClass(name, this.customVersion, isExplicit);
    }

    @Override
    public Class<? extends Group> getGroupClass(String name, String version) throws HL7Exception {
        return super.getGroupClass(name, this.customVersion);
    }

}
