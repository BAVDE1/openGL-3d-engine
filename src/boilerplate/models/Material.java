package boilerplate.models;

import boilerplate.rendering.ShaderProgram;
import boilerplate.rendering.textures.Texture2d;
import org.joml.Vector3f;

public class Material {
    public Vector3f ambient;
    public Vector3f diffuse;
    public Vector3f specular;
    public Float shininess;

    public Texture2d diffuseTexture;
    public Texture2d specularMap;

    public Material() {
    }

    public Material(Vector3f colour) {
        this.ambient = colour;
        this.diffuse = colour;
    }

    public Material(Vector3f colour, float shininess) {
        this(colour);
        this.shininess = shininess;
    }

    public Material(Texture2d diffuseTexture) {
        this.diffuseTexture = diffuseTexture;
    }

    public Material(Texture2d diffuseTexture, Texture2d specularMap) {
        this.diffuseTexture = diffuseTexture;
        this.specularMap = specularMap;
    }

    public Material(Vector3f ambient, Vector3f diffuse, Vector3f specular) {
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
    }

    public Material(Vector3f ambient, Vector3f diffuse, Vector3f specular, Texture2d diffuseTexture) {
        this(ambient, diffuse, specular);
        this.diffuseTexture = diffuseTexture;
    }

    public void uniformValues(String uniform, ShaderProgram sh) {
        if (ambient != null) sh.uniform3f(uniform + ".ambient", ambient);
        if (diffuse != null) sh.uniform3f(uniform + ".diffuse", diffuse);
        if (specular != null) sh.uniform3f(uniform + ".specular", specular);
        if (shininess != null) sh.uniform1f(uniform + ".shininess", shininess);
    }

    public void uniformAndBindTextures(String uniform, ShaderProgram sh, int textureSlotStart) {
        int texSlot = textureSlotStart;
        if (diffuseTexture != null) sh.uniformTexture(uniform + ".diffuseTexture", diffuseTexture, texSlot++);
        if (specularMap != null) sh.uniformTexture(uniform + ".specularMap", specularMap, texSlot++);
    }

    @Override
    public String toString() {
        return "Material(amb: %s, dif: %s, spec: %s, %s)".formatted(ambient, diffuse, specular, diffuseTexture);
    }
}
