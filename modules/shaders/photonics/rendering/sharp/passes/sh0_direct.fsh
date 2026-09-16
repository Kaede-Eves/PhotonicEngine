#version 430

#define FRAG_USE_RT_POS
#define FRAG_USE_GEO_NORMAL
#define FRAG_USE_TEX_NORMAL

#include "/photonics/rendering/frag/common.glsl"
#include "/photonics/light_list.glsl"
#include "/photonics/tracing.glsl"

layout(location = 0) out vec4 sharp_diffuse;

/*
    Direct lighting, no temporal state.

    Every light in the list is traced for this fragment, in list order, until PH_MAX_SAMPLES of
    them have actually contributed. That determinism is the point: with no reservoir and no
    history there is nothing to converge, so a light change shows up the frame its voxels land
    and the result carries no sampling noise.

    The trade against ReSTIR is cost and softness. Cost scales with the number of emitters in
    range rather than with screen resolution, and PH_MAX_SAMPLES is the only cap on it. Shadows
    are hard, because a single ray is traced to each light's centre, and there is no indirect
    bounce at all.
*/
void main() {
    setup_frag_data(0);
    if (!frag_is_in_world) discard;

    vec3 diffuse = vec3(0.0f);
    int contributed = 0;

    for (int i = 0; i < light_list_size && contributed < PH_MAX_SAMPLES; i++) {
        Light light = light_list_get(i);
        if (light.type == LIGHT_TYPE_INVALID) continue;

        vec3 to_light = light.position - frag_rt_pos;

        // Cheap rejections before spending a ray: behind the surface, or beyond the light's reach.
        if (dot(to_light, frag_geo_normal) <= 0.0f) continue;
        if (dot(to_light, to_light) > light.block_radius * light.block_radius) continue;

        vec3 attenuated = ph_compute_attenuation(
            light,
            to_light,
            frag_rt_pos,
            light.position,
            frag_geo_normal,
            frag_tex_normal
        );

        if (dot(attenuated, attenuated) <= 0.0f) continue;

        vec3 tint_color;
        float light_transmittance;

        // Same iteration budget the ReSTIR visibility check uses.
        if (!trace_light_vis(frag_rt_pos, to_light, light.position, 40, tint_color, light_transmittance))
            continue;

        // Same combination the ReSTIR path uses: tint from anything translucent the ray passed
        // through, scaled by how much of the ray survived.
        //
        // Deliberately no light.intensity factor. The colour uploaded for a light is already
        // premultiplied by it (BlockLightInfo.getColorAsVector multiplies by adjustedIntensity),
        // and .w carries the same factor a second time for nothing -- no shader in the pack reads
        // it. Multiplying here applied intensity twice and, since it is well under 1.0 for most
        // emitters, squared the output down to roughly nothing.
        diffuse += attenuated * tint_color * light_transmittance;
        contributed++;
    }

    sharp_diffuse = vec4(diffuse, 1.0f);
}
