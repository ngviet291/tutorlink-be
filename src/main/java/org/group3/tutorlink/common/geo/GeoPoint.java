package org.group3.tutorlink.common.geo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
//Một class đại diện cho một điểm địa lý với vĩ độ và kinh độ.
@Getter
@RequiredArgsConstructor
public class GeoPoint {

    private final double latitude;
    private final double longitude;
}
