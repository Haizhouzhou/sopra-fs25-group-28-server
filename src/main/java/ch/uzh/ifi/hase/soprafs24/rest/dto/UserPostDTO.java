package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class UserPostDTO {

  private String name;

  private String username;

  private String password;

  private String token;

  private String avatar;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private Date birthday;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword(){
    return password;
  }

  public void setPassword(String password){
    this.password = password;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }


  public Date getBirthday(){
    return birthday;
  }

  public void setBirthday(Date birthday){
    this.birthday = birthday;
  }

  public String getAvatar() {return avatar;}

  public void setAvatar(String avatar) {this.avatar = avatar;}
  
}
