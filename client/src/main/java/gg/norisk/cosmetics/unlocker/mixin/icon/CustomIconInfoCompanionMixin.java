package gg.norisk.cosmetics.unlocker.mixin.icon;

import gg.norisk.cosmetics.unlocker.cosmetics.CosmeticsUnlocker;
import gg.norisk.networking.model.cosmetics.CustomIcon;
import gg.norisk.networking.model.cosmetics.CustomIconInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "gg.norisk.networking.model.cosmetics.CustomIconInfo$Companion", remap = false)
public abstract class CustomIconInfoCompanionMixin {

    @Inject(method = "getICONS", at = @At("HEAD"), cancellable = true, remap = false)
    private void returnAllIcons(CallbackInfoReturnable<List<CustomIcon>> cir) {
        if (CosmeticsUnlocker.isEnabled()) {
            CustomIconInfo.Companion comp = CustomIconInfo.Companion;
            List<CustomIcon> all = new ArrayList<>();
            all.add(comp.getDEFAULT());
            all.add(comp.getBETA());
            all.add(comp.getVIP());
            all.add(comp.getGOLD_DONATOR());
            all.add(comp.getSILVER_DONATOR());
            all.add(comp.getBRONZE_DONATOR());
            all.add(comp.getDEVELOPER());
            all.add(comp.getADMIN());
            all.add(comp.getPARTNER());
            all.add(comp.getSPECIAL_RANK());
            all.add(comp.getBUG_HUNTER());
            all.add(comp.getGIFTER());
            all.add(comp.getHELPER());
            all.add(comp.getDESIGNER());
            all.add(comp.getCREATOR_CODE());
            all.add(comp.getBLACK_WEEK_V2());
            all.add(comp.getBLACK_WEEK());
            all.add(comp.getXMAS());
            all.add(comp.getCHRISTMAS_2025());
            all.add(comp.getHALLOWEEN_2025());
            all.add(comp.getVALENTINES());
            all.add(comp.getGAMESCOM_2025());
            all.add(comp.getGAMESCOM_2026());
            cir.setReturnValue(all);
        }
    }
}
