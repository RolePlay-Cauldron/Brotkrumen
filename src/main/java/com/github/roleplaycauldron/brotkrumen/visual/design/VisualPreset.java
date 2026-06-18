package com.github.roleplaycauldron.brotkrumen.visual.design;

import java.util.Optional;

/**
 * File-backed visual preset with independent renderer-specific design sets.
 *
 * @param name            normalized preset name
 * @param spellbookEffect optional Spellbook effect renderer design set
 * @param blockDisplay    optional block-display renderer design set
 */
public record VisualPreset(String name, ParticleDesignSet spellbookEffect,
                           BlockDisplayDesignSet blockDisplay) {

    /**
     * Checks whether this preset supports a renderer.
     *
     * @param renderer renderer
     * @return true when renderer data is present
     */
    public boolean supports(final VisualRenderer renderer) {
        return switch (renderer) {
            case SPELLBOOK_EFFECT -> spellbookEffect != null;
            case BLOCK_DISPLAY -> blockDisplay != null;
        };
    }

    /**
     * Returns Spellbook effect design data when present.
     *
     * @return optional design set
     */
    public Optional<ParticleDesignSet> spellbookEffectDesign() {
        return Optional.ofNullable(spellbookEffect);
    }

    /**
     * Returns block-display design data when present.
     *
     * @return optional design set
     */
    public Optional<BlockDisplayDesignSet> blockDisplayDesign() {
        return Optional.ofNullable(blockDisplay);
    }
}
