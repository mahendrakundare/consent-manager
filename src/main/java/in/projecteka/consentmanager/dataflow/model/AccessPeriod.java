package in.projecteka.consentmanager.dataflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@AllArgsConstructor
@Builder
public class AccessPeriod {
    @JsonProperty("from")
    @NotNull(message = "From Date is not specified.")
    private final Date fromDate;

    @JsonProperty("to")
    @NotNull(message = "To Date is not specified.")
    private final Date toDate;
}
