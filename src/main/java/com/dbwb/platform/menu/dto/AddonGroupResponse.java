package com.dbwb.platform.menu.dto;

import com.dbwb.platform.menu.entity.Addon;
import com.dbwb.platform.menu.entity.AddonGroup;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AddonGroupResponse(UUID id, String name, Integer maxSelections, List<AddonResponse> addons) {
    public record AddonResponse(UUID id, String name, BigDecimal extraPrice) {
        public static AddonResponse from(Addon a) {
            return new AddonResponse(a.getId(), a.getName(), a.getExtraPrice());
        }
    }

    public static AddonGroupResponse from(AddonGroup group, List<Addon> addons) {
        return new AddonGroupResponse(group.getId(), group.getName(), group.getMaxSelections(),
                addons.stream().map(AddonResponse::from).toList());
    }
}
