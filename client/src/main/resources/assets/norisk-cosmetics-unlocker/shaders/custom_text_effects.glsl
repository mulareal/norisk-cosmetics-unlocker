// Custom nametag text effects. Spliced into NoRisk's nrc_text_effects_v2.glsl by CustomTextEffects:
// this block goes in front of applyTextEffects(), the dispatch lines (DISPATCH) go into it.
// Same building blocks as NoRisk's own effects: textData, GameTime, random/noise, hsvToRgb, textSdf.

float nrcu_or1(float v) { return v > 0.001 ? v : 1.0; }

vec3 nrcu_color_or(vec3 color, vec3 fallback) { return dot(color, vec3(1.0)) < 0.01 ? fallback : color; }

// Letters hop one after another.
void nrcu_bounce(float speed, float intensity) {
    speed = nrcu_or1(speed);
    float t = GameTime * 6000.0 * speed - textData.characterPosition.x * 0.35;
    float hop = pow(abs(sin(t)), 0.7);
    textData.uv.y += hop * 1.6 * nrcu_or1(intensity) / 256.0;
    // Slight squash when the letter lands
    float squash = 1.0 - smoothstep(0.0, 0.25, hop);
    textData.uv.x = textData.uvCenter.x + (textData.uv.x - textData.uvCenter.x) * (1.0 - 0.12 * squash);
    textData.shouldScale = true;
}

// Short bursts: horizontal slice offsets, red/cyan split, flicker.
void nrcu_glitch(float speed, float intensity) {
    textData.shouldScale = true;
    float tick = floor(GameTime * 24000.0 * nrcu_or1(speed) * 0.6);
    float burst = step(0.6, random(vec2(tick, 3.7)));
    float band = floor((textData.uv.y - textData.uvMin.y) * 128.0);
    float shift = (random(vec2(tick + band * 7.13, floor(textData.characterPosition.x))) - 0.5) * 3.0;
    textData.uv.x += shift * nrcu_or1(intensity) * burst / 256.0;
    if (burst < 0.5) return;
    if (textData.isShadow) { textData.color.a = 0.0; return; }

    vec2 off = vec2(1.0 / 256.0, 0.0);
    vec4 a = texture(Sampler0, textData.uv + off);
    if (uvBoundsCheck(textData.uv + off, textData.uvMin, textData.uvMax)) a = vec4(0.0);
    vec4 b = texture(Sampler0, textData.uv - off);
    if (uvBoundsCheck(textData.uv - off, textData.uvMin, textData.uvMax)) b = vec4(0.0);
    textData.backColor = max(vec4(1.0, 0.15, 0.55, 1.0) * a.a, vec4(0.1, 0.9, 1.0, 1.0) * b.a);
    if (random(vec2(tick, 9.1)) > 0.8) textData.color.a *= 0.35;
}

// Bright core with a pulsing glow in color 1.
void nrcu_neon(vec3 color, float speed, float intensity) {
    if (textData.isShadow) { textData.color.a = 0.0; return; }
    color = nrcu_color_or(color, rgb(0, 240, 255));
    float pulse = 0.55 + 0.45 * sin(GameTime * 6000.0 * nrcu_or1(speed));
    textData.color.rgb = mix(color, vec3(1.0), 0.3 + 0.3 * pulse);
    vec3 d = textSdf();
    float glow = (1.0 - d.z) * (1.0 - d.z);
    textData.backColor = vec4(color, glow * pulse * min(nrcu_or1(intensity), 1.5));
}

// Every letter its own color, moving along a wave.
void nrcu_rainbow_wave(float speed, float intensity) {
    speed = nrcu_or1(speed);
    float phase = textData.characterPosition.x * 0.18 - GameTime * 7500.0 * speed;
    textData.uv.y += sin(phase) * 1.2 * nrcu_or1(intensity) / 256.0;
    textData.shouldScale = true;
    textData.color.rgb = hsvToRgb(vec3(textData.characterPosition.x * 0.02 - GameTime * 400.0 * speed, 0.75, 1.0));
    if (textData.isShadow) textData.color.rgb *= 0.25;
}

// Sparks rising through each letter (color 1). Works in letter-local coordinates (localPosition: 0..1
// over the glyph, y down), seeded by the glyph's atlas position, so nothing depends on camera or distance.
float nrcu_spark_field(vec2 lp, vec2 seed, float speed) {
    float s = 0.0;
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float t = GameTime * 1200.0 * speed * (0.8 + 0.4 * random(seed + fi * 3.1)) + random(seed + fi * 7.7);
        float cycle = floor(t);
        float life = fract(t);
        float x = 0.1 + 0.8 * random(seed + vec2(cycle, fi * 5.3));
        vec2 pos = vec2(x + 0.08 * sin(life * TAU + fi * 2.0), 1.0 - 1.1 * life);
        float r = length((lp - pos) * vec2(6.0, 8.0));
        s = max(s, smoothstep(0.9, 0.2, r) * sin(life * PI));
    }
    return s;
}

void nrcu_sparks(vec3 color, float speed, float intensity) {
    if (textData.isShadow) return;
    color = nrcu_color_or(color, rgb(255, 190, 60));
    vec2 seed = floor(textData.uvMin * 256.0);
    float s = nrcu_spark_field(textData.localPosition, seed, nrcu_or1(speed)) * min(nrcu_or1(intensity), 1.5);
    vec3 c = mix(color, vec3(1.0), s * s);
    if (s > textData.backColor.a) textData.backColor = vec4(c, s);
    if (s > textData.topColor.a) textData.topColor = vec4(c, s);
}
