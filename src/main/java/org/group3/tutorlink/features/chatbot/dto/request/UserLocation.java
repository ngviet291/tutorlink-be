package org.group3.tutorlink.features.chatbot.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.group3.tutorlink.common.entity.Address;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserLocation {

    private String ward;

    private String province;

    public static UserLocation unknown() {
        return new UserLocation();
    }

    public static UserLocation fromAddress(Address address) {
        return address == null ? unknown() : new UserLocation(address.getWard(), address.getProvince());
    }

    @JsonIgnore
    public boolean isKnown() {
        return province != null && !province.isBlank();
    }
}
