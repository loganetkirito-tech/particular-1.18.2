package org.community.particular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.Random;

/** Partial water FX adaptation: source textures originate with Particular. */
@Mod("particularwater1182")
public class ParticularWater {
    private static final String ID="particularwater1182";
    private static final DeferredRegister<ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ID);
    public static final RegistryObject<SimpleParticleType> SPLASH=PARTICLES.register("water_splash",()->new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> RIPPLE=PARTICLES.register("water_ripple",()->new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> FOAM=PARTICLES.register("water_splash_foam",()->new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SPRAY=PARTICLES.register("waterfall_spray",()->new SimpleParticleType(false));
    public ParticularWater(){IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus(); PARTICLES.register(bus);}
    @Mod.EventBusSubscriber(modid="particularwater1182",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class FactoryRegister {
        @SubscribeEvent public static void setup(ParticleFactoryRegisterEvent e){
            Minecraft mc=Minecraft.getInstance();
            if (mc.particleEngine == null) return;
            mc.particleEngine.register(SPLASH.get(), s->new Factory(s,0));
            mc.particleEngine.register(RIPPLE.get(), s->new Factory(s,1));
            mc.particleEngine.register(FOAM.get(), s->new Factory(s,2));
            mc.particleEngine.register(SPRAY.get(), s->new Factory(s,3));
        }
    }
    @Mod.EventBusSubscriber(modid="particularwater1182",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.FORGE)
    public static class Spawner {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.END)return;
            Minecraft mc=Minecraft.getInstance();ClientLevel lvl=mc.level;
            if(lvl==null||mc.player==null||mc.isPaused())return;
            Random rng=lvl.random;
            // Nearby water surfaces: subtle ripples; no changes to world blocks.
            for(int i=0;i<10;i++){
                int x=Mth.floor(mc.player.getX())+rng.nextInt(25)-12;
                int z=Mth.floor(mc.player.getZ())+rng.nextInt(25)-12;
                int y=Mth.floor(mc.player.getY())+rng.nextInt(17)-8;
                BlockPos p=new BlockPos(x,y,z);
                if(!lvl.getFluidState(p).is(Fluids.WATER) || !lvl.getBlockState(p.above()).isAir())continue;
                if(rng.nextInt(7)==0) lvl.addParticle(RIPPLE.get(),x+rng.nextDouble(),y+1.015,z+rng.nextDouble(),0,0,0);
                // Falling fluid column ending in a surface: spray near short waterfalls.
                BlockPos above=p.above(2);
                if(rng.nextInt(4)==0 && lvl.getFluidState(above).is(Fluids.WATER) && lvl.getBlockState(p.above()).isAir())
                    lvl.addParticle(SPRAY.get(),x+rng.nextDouble(),y+1.02,z+rng.nextDouble(),0,0.025,0);
            }
            // Splash around nearby entities entering water, visually driven by their motion.
            for(Entity ent : lvl.getEntities(mc.player,mc.player.getBoundingBox().inflate(12))) {
                if(ent.isInWater() && Math.abs(ent.getDeltaMovement().y)>0.12 && rng.nextInt(3)==0){
                    double surface=ent.getY()+0.15;
                    for(int j=0;j<3;j++)lvl.addParticle(SPLASH.get(),ent.getX()+(rng.nextDouble()-.5),surface,ent.getZ()+(rng.nextDouble()-.5), (rng.nextDouble()-.5)*.13,0.08+rng.nextDouble()*.06,(rng.nextDouble()-.5)*.13);
                    lvl.addParticle(FOAM.get(),ent.getX(),surface,ent.getZ(),0,0,0);
                }
            }
        }
    }
    private static final class Factory implements ParticleProvider<SimpleParticleType>{
        private final SpriteSet sprites;private final int kind;
        Factory(SpriteSet sprites,int kind){this.sprites=sprites;this.kind=kind;}
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double xd,double yd,double zd){
            return new FxParticle(level,x,y,z,xd,yd,zd,sprites,kind);
        }
    }
    private static final class FxParticle extends TextureSheetParticle{
        private final SpriteSet sprites;private final int kind;
        FxParticle(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,SpriteSet sprites,int kind){
            super(level,x,y,z,vx,vy,vz);this.sprites=sprites;this.kind=kind;
            this.hasPhysics=false;this.gravity=kind==0?0.32f:0f;this.friction=0.9f;
            this.xd=vx;this.yd=vy;this.zd=vz;
            this.lifetime=kind==1?14:kind==2?12:kind==3?17:10;
            this.quadSize=kind==1?0.65f:kind==2?0.36f:kind==3?0.24f:0.3f;
            this.alpha=kind==1?0.52f:0.82f;
            this.setSpriteFromAge(sprites);
        }
        @Override public void tick(){super.tick();setSpriteFromAge(sprites);if(age>lifetime-5)alpha=Math.max(0f,(lifetime-age)/5f)*(kind==1?.52f:.82f);}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    }
}
