package dev.mja00.swarmsmps2.mixin;

import net.minecraft.client.multiplayer.resolver.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerNameResolver.class)
public class OptimizeBlockedServerCheck {

    @Shadow
    @Final
    private ServerRedirectHandler redirectHandler;
    private AddressCheck addressCheck;
    private ServerAddressResolver resolver;

    @Inject(method = "resolveAddress(Lnet/minecraft/client/multiplayer/resolver/ServerAddress;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void optimizedResolveAddress(ServerAddress pServerAddress, CallbackInfoReturnable<Optional<ResolvedServerAddress>> cir) {
        // Check if the IP is numeric
        boolean isNumeric = pServerAddress.getHost().matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
        Optional<ResolvedServerAddress> optional = this.resolver.resolve(pServerAddress);
        if (isNumeric) {
            // We only need to check the blacklist for the numeric IP
            if (!this.addressCheck.isAllowed(pServerAddress)) {
                // It's blacklisted so return an empty optional
                cir.setReturnValue(Optional.empty());
            }
            // Return it
            cir.setReturnValue(optional);
        } else {
            // We're dealing with a domain name
            if (optional.isEmpty() || this.addressCheck.isAllowed(optional.get())) {
                // It's not blacklisted so check for a SRV record
                Optional<ServerAddress> optional1 = this.redirectHandler.lookupRedirect(pServerAddress);
                if (optional1.isPresent()) {
                    // We have a SRV record
                    optional = this.resolver.resolve(optional1.get()).filter(this.addressCheck::isAllowed);
                }

                // Return it
                cir.setReturnValue(optional);
            } else {
                // It's blacklisted so return an empty optional
                cir.setReturnValue(Optional.empty());
            }
        }
    }

}
