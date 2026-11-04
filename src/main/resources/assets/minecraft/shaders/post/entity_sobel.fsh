#version 330
#extension GL_ARB_separate_shader_objects : require

// Vanilla entity_sobel with one change: single-pixel gaps in the outline buffer are closed
// before the edge detection. Very large entities (e.g. a scaled-up Gomme holding a sword)
// get such gaps from GPU clipping, and vanilla turns each of them into a flickering white dot.

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

// Sample at uv, treating a pixel as covered if both of its horizontal or both of its vertical neighbors are.
// A covered gap takes the color of the neighbors that close it, so it does not darken the glow.
vec4 closedSample(vec2 uv, vec2 oneTexel) {
    vec4 center = texture(InSampler, uv);
    vec4 left = texture(InSampler, uv - vec2(oneTexel.x, 0.0));
    vec4 right = texture(InSampler, uv + vec2(oneTexel.x, 0.0));
    vec4 up = texture(InSampler, uv - vec2(0.0, oneTexel.y));
    vec4 down = texture(InSampler, uv + vec2(0.0, oneTexel.y));
    float horizontal = min(left.a, right.a);
    float vertical = min(up.a, down.a);
    float closed = max(horizontal, vertical);
    if (center.a >= closed) return center;
    vec3 fill = horizontal >= vertical ? (left.rgb + right.rgb) * 0.5 : (up.rgb + down.rgb) * 0.5;
    return vec4(fill, closed);
}

void main(){
    vec2 oneTexel = 1.0 / InSize;

    vec2 leftUv = texCoord - vec2(oneTexel.x, 0.0);
    vec2 rightUv = texCoord + vec2(oneTexel.x, 0.0);
    vec2 upUv = texCoord - vec2(0.0, oneTexel.y);
    vec2 downUv = texCoord + vec2(0.0, oneTexel.y);

    vec4 center = closedSample(texCoord, oneTexel);
    vec4 left = closedSample(leftUv, oneTexel);
    vec4 right = closedSample(rightUv, oneTexel);
    vec4 up = closedSample(upUv, oneTexel);
    vec4 down = closedSample(downUv, oneTexel);

    float leftDiff  = abs(center.a - left.a);
    float rightDiff = abs(center.a - right.a);
    float upDiff    = abs(center.a - up.a);
    float downDiff  = abs(center.a - down.a);
    float total = clamp(leftDiff + rightDiff + upDiff + downDiff, 0.0, 1.0);
    vec3 outColor = center.rgb * center.a + left.rgb * left.a + right.rgb * right.a + up.rgb * up.a + down.rgb * down.a;
    fragColor = vec4(outColor * 0.2, total);
}
