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

// Alpha at uv, treating a pixel as covered if both of its horizontal or both of its vertical neighbors are.
float closedAlpha(vec2 uv, vec2 oneTexel) {
    float alpha = texture(InSampler, uv).a;
    float horizontal = min(texture(InSampler, uv - vec2(oneTexel.x, 0.0)).a, texture(InSampler, uv + vec2(oneTexel.x, 0.0)).a);
    float vertical = min(texture(InSampler, uv - vec2(0.0, oneTexel.y)).a, texture(InSampler, uv + vec2(0.0, oneTexel.y)).a);
    return max(alpha, max(horizontal, vertical));
}

void main(){
    vec2 oneTexel = 1.0 / InSize;

    vec2 leftUv = texCoord - vec2(oneTexel.x, 0.0);
    vec2 rightUv = texCoord + vec2(oneTexel.x, 0.0);
    vec2 upUv = texCoord - vec2(0.0, oneTexel.y);
    vec2 downUv = texCoord + vec2(0.0, oneTexel.y);

    vec4 center = texture(InSampler, texCoord);
    vec4 left = texture(InSampler, leftUv);
    vec4 right = texture(InSampler, rightUv);
    vec4 up = texture(InSampler, upUv);
    vec4 down = texture(InSampler, downUv);

    float centerA = closedAlpha(texCoord, oneTexel);
    float leftA = closedAlpha(leftUv, oneTexel);
    float rightA = closedAlpha(rightUv, oneTexel);
    float upA = closedAlpha(upUv, oneTexel);
    float downA = closedAlpha(downUv, oneTexel);

    float leftDiff  = abs(centerA - leftA);
    float rightDiff = abs(centerA - rightA);
    float upDiff    = abs(centerA - upA);
    float downDiff  = abs(centerA - downA);
    float total = clamp(leftDiff + rightDiff + upDiff + downDiff, 0.0, 1.0);
    vec3 outColor = center.rgb * center.a + left.rgb * left.a + right.rgb * right.a + up.rgb * up.a + down.rgb * down.a;
    fragColor = vec4(outColor * 0.2, total);
}
