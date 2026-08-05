#version 150

// Paints the front face of one body part from a player's skin and stains it by damage:
//   uHealth == 1  -> untouched skin
//   uHealth == 0  -> desaturated + blood-tinted (a wrecked limb)
// The skin sub-region for this part is passed in so the whole 64x64 skin can be one bound texture.

uniform sampler2D Skin;
uniform vec4 uBase;      // xy = base-layer region origin (0..1), zw = size (0..1)
uniform vec4 uOverlay;   // xy = overlay-layer (hat/jacket/sleeve/pants) origin, zw = size
uniform float uHealth;   // 0 = destroyed, 1 = healthy

in vec2 texCoord;
out vec4 fragColor;

void main() {
    // GUI texCoord's v is flipped relative to the skin's v axis, so sample with y inverted to draw upright.
    vec2 tc = vec2(texCoord.x, 1.0 - texCoord.y);
    vec2 baseUV = uBase.xy + tc * uBase.zw;
    vec4 base = texture(Skin, baseUV);

    vec2 ovUV = uOverlay.xy + tc * uOverlay.zw;
    vec4 ov = texture(Skin, ovUV);

    vec3 rgb = mix(base.rgb, ov.rgb, ov.a);
    float alpha = max(base.a, ov.a);

    float dmg = clamp(1.0 - uHealth, 0.0, 1.0);
    float lum = dot(rgb, vec3(0.299, 0.587, 0.114));
    vec3 damaged = mix(rgb, vec3(lum), dmg * 0.85);
    damaged = mix(damaged, vec3(0.5, 0.03, 0.03), dmg * 0.55);

    fragColor = vec4(damaged, alpha);
}
