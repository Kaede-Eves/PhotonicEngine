//ph_required: uniform sampler2D sharp_diffuse;

#if defined PH_ENABLE_HANDHELD_LIGHT
//ph_required: uniform bool off_hand_has_light, main_hand_has_light;
//ph_required: uniform sampler2D handheld_diffuse;
#endif

vec3 sample_photonics_direct(vec2 tex_coord) {
    return texture(sharp_diffuse, tex_coord).rgb;
}

vec3 sample_photonics_handheld(vec2 tex_coord) {
#if defined PH_ENABLE_HANDHELD_LIGHT
    return (main_hand_has_light || off_hand_has_light) ? texture(handheld_diffuse, tex_coord).rgb : vec3(0.0f);
#else
    return vec3(0.0f);
#endif
}
