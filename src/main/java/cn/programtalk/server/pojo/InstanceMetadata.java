package cn.programtalk.server.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class InstanceMetadata implements Serializable {
    private String url;
    private String name;

    @Override
    public String toString() {
        return "InstanceMetadata{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
