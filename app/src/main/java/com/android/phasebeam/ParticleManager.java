package com.android.phasebeam;

import java.util.Random;

public class ParticleManager
{
    public static class Particle
    {
        float x;
        float y;
        float z;

        Particle(float[] data)
        {
            x = data[0];
            y = data[1];
            z = data[2];
        }

        public float[] toFloatArray()
        {
            return new float[]{x, y, z};
        }
    }

    Random random = new Random();

    //region Particle data
        private final int particleCount = 26;
        private final int particlePropertyCount = 3;
        private final int particleArrayLength = particleCount * particlePropertyCount;
        private final int particleArrayDataLength = particleArrayLength * 4;
        private final float[] particleData = new float[particleArrayLength];
        private final float[] beamData = new float[particleArrayLength];
    //endregion

    //region Dimensional data
        public float xOffset = 0.5f;
        private static float oldXOffset = 0.5f;
        private static float newXOffset = 0.5f;
        public float backgroundXOffset = 0.0f;
        public float particleXOffset = 0.0f;
    //endregion

    ParticleManager()
    {
        initializeParticles();
    }

    public int getParticleArrayDataLength()
    {
        return this.particleArrayDataLength;
    }

    public float[] getParticleData()
    {
        return particleData;
    }

    public float[] getBeamData()
    {
        return beamData;
    }

    public int getParticleCount()
    {
        return particleCount;
    }

    public void setXOffset(float xOffset)
    {
        this.xOffset = xOffset;
    }

    private void initializeParticles()
    {
        for (int i = 0; i < particleCount; i++)
        {
            int index = i * particlePropertyCount;

            Particle particle = new Particle(new float[particlePropertyCount]);

            particle.x = boundRandom(-1.25f, 1.25f);
            particle.y = boundRandom(-1.25f, 1.25f);

            float z;

            if (i < 3)
            {
                z = 14.0f;
            }
            else if (i < 4)
            {
                z = boundRandom(10.0f, 20.0f);
            }
            else if (i < 7)
            {
                z = 25.0f;
            }
            else if (i == 10)
            {
                z = 24.0f;
                particle.x = 1.0f;
            }
            else
            {
                z = boundRandom(6.0f, 14.0f);
            }

            particle.z = z;


            float[] newParticle = particle.toFloatArray();
            System.arraycopy(newParticle, 0, particleData, index, particlePropertyCount);
        }

        for(int i = 0; i < particleCount; i++)
        {
            int index = i * particlePropertyCount;

            Particle beam = new Particle(new float[particlePropertyCount]);

            float z;

            if(i < 20)
            {
                z = boundRandom(4.0f, 10.0f) / 2.0f;
            }
            else
            {
                z = boundRandom(4.0f, 35.0f) / 2.0f;
            }

            beam.x = boundRandom(-1.25f, 1.25f);
            beam.y = boundRandom(-1.05f, 1.205f);

            beam.z = z;

            float[] newBeam = beam.toFloatArray();
            System.arraycopy(newBeam, 0, beamData, index, particlePropertyCount);
        }
    }

    public void updateParticles()
    {
        for (int i = 0; i < particleCount; i++)
        {
            int index = i * particlePropertyCount;

            float[] initialRawParticleData = new float[particlePropertyCount];
            System.arraycopy(particleData, index, initialRawParticleData, 0, particlePropertyCount);

            Particle particle = new Particle(initialRawParticleData);

            float[] initialRawBeamData = new float[particlePropertyCount];
            System.arraycopy(beamData, index, initialRawBeamData, 0, particlePropertyCount);

            Particle beam = new Particle(initialRawBeamData);

            if(newXOffset == oldXOffset) {
                if (beam.x / beam.z > 0.5f) {
                    beam.x = -1.0f;
                }

                if (particle.x / particle.z > 0.5f)
                {
                    particle.x = -1.0f;
                }

                if (beam.y > 1.05f)
                {
                    beam.y = -1.05f;
                    beam.x = boundRandom(-1.25f, 1.25f);
                }
                else
                {
                    beam.y = beam.y + 0.000160f * beam.z;
                }

                if (particle.y > 1.25f)
                {
                    particle.y = -1.25f;
                    particle.x = boundRandom(-1.25f, 1.25f);
                }
                else
                {
                    particle.y = particle.y + 0.00022f * particle.z;
                }
            }

            beam.x = beam.x + 0.0001f * beam.z;

            float[] updatedRawBeamData = beam.toFloatArray();
            System.arraycopy(updatedRawBeamData, 0, beamData, index, particlePropertyCount);

            // the next beams z value because the renderscript can use pointer magic but we can't
            if (i < particleCount - 1)
            {
                beam.z = beamData[(i + 1) * particlePropertyCount + 2];
            }
            else
            {
                beam.z = beamData[2];
            }

            particle.x = particle.x + 0.0001560f * beam.z;

            float[] updatedRawParticleData = particle.toFloatArray();
            System.arraycopy(updatedRawParticleData, 0, particleData, index, particlePropertyCount);
        }

        particleXOffset = newXOffset;
    }

    public void tickXOffset()
    {
        newXOffset = xOffset * 2;

        if (newXOffset != oldXOffset)
        {
            backgroundXOffset = -xOffset / 2.0f;
        }
    }

    public void finishXOffseTick()
    {
        oldXOffset = newXOffset;
    }

    private float boundRandom(float min, float max)
    {
        return min + random.nextFloat() * (max - min);
    }
}
