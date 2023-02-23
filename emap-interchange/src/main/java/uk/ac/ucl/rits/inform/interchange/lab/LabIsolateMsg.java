package uk.ac.ucl.rits.inform.interchange.lab;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import uk.ac.ucl.rits.inform.interchange.InterchangeValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represent information about microbial isolates.
 * @author Stef Piatek
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class LabIsolateMsg implements Serializable {

    private String epicCareOrderNumber;
    /**
     * Id for the individual isolate, as the code and name can change.
     */
    private String isolateId;

    private String isolateCode;
    private String isolateName;
    private InterchangeValue<String> cultureType = InterchangeValue.unknown();
    private InterchangeValue<String> quantity = InterchangeValue.unknown();
    private InterchangeValue<String> clinicalInformation = InterchangeValue.unknown();

    private List<LabResultMsg> sensitivities = new ArrayList<>();

    public LabIsolateMsg() {
    }

    /**
     * Create minimal LabIsolateMsg.
     * @param epicCareOrderNumber epic order number for isolate
     * @param isolateId           WinPath isolate ID code
     */
    public LabIsolateMsg(String epicCareOrderNumber, String isolateId) {
        this.epicCareOrderNumber = epicCareOrderNumber;
        this.isolateId = isolateId;
    }

    /**
     * merge isolate information excluding sensitivities.
     * @param other other lab isolate message
     */
    @JsonIgnore
    public void mergeIsolateInfo(LabIsolateMsg other) {
        assert epicCareOrderNumber.equals(other.epicCareOrderNumber);
        assert isolateId.equals(other.isolateId);

        isolateCode = other.isolateCode != null ? other.isolateCode : isolateCode;
        isolateName = other.isolateName != null ? other.isolateName : isolateName;
        cultureType = other.cultureType.isSave() ? other.cultureType : cultureType;
        quantity = other.quantity.isSave() ? other.quantity : quantity;
        clinicalInformation = other.clinicalInformation.isSave() ? other.clinicalInformation : clinicalInformation;

    }
}
