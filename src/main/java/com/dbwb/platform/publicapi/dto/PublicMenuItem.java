package com.dbwb.platform.publicapi.dto;

import com.dbwb.platform.menu.entity.Addon;
import com.dbwb.platform.menu.entity.AddonGroup;
import com.dbwb.platform.menu.entity.BoxVariant;
import com.dbwb.platform.menu.entity.ItemAvailability;
import com.dbwb.platform.menu.entity.MenuItem;
import com.dbwb.platform.menu.entity.SizeVariant;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Read-only, public-safe projection of a menu item - no internal ids beyond what the cart needs. */
public record PublicMenuItem(
        UUID id,
        String name,
        String description,
        String ingredients,
        BigDecimal price,
        BigDecimal discountPrice,
        String imageUrl,
        ItemAvailability availability,
        Integer maxOrderQuantity,
        boolean fixedBoxItem,
        List<SizeOption> sizes,
        List<AddonGroupOption> addonGroups,
        List<BoxOption> boxVariants
) {
    public record SizeOption(UUID id, String label, BigDecimal price) {
        static SizeOption from(SizeVariant s) {
            return new SizeOption(s.getId(), s.getLabel(), s.getPrice());
        }
    }

    public record AddonOption(UUID id, String name, BigDecimal extraPrice) {
        static AddonOption from(Addon a) {
            return new AddonOption(a.getId(), a.getName(), a.getExtraPrice());
        }
    }

    public record AddonGroupOption(UUID id, String name, Integer maxSelections, List<AddonOption> options) {
    }

    public record BoxOption(UUID id, String label, int unitCount, BigDecimal price) {
        static BoxOption from(BoxVariant b) {
            return new BoxOption(b.getId(), b.getLabel(), b.getUnitCount(), b.getPrice());
        }
    }

    public static PublicMenuItem from(MenuItem item, List<SizeVariant> sizes,
                                       List<AddonGroup> groups, java.util.Map<UUID, List<Addon>> addonsByGroup,
                                       List<BoxVariant> boxVariants) {
        List<AddonGroupOption> groupOptions = groups.stream()
                .map(g -> new AddonGroupOption(
                        g.getId(), g.getName(), g.getMaxSelections(),
                        addonsByGroup.getOrDefault(g.getId(), List.of()).stream().map(AddonOption::from).toList()))
                .toList();

        return new PublicMenuItem(
                item.getId(), item.getName(), item.getDescription(), item.getIngredients(),
                item.getPrice(), item.getDiscountPrice(), item.getImageUrl(), item.getAvailability(),
                item.getMaxOrderQuantity(), item.isFixedBoxItem(),
                sizes.stream().map(SizeOption::from).toList(),
                groupOptions,
                boxVariants.stream().map(BoxOption::from).toList());
    }
}
