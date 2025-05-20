package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

public class UserListGetDTO {
  //pack information needed for users overwill page
  private String name;

  private String username;

  private UserStatus status;

  private Long id;

  private String avatar;

  private int wincounter;

  public Long getId(){
    return id;
  }

  public void setId(Long id){
    this.id = id;
  }

  
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

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public String getAvatar() {return avatar;}

  public void setAvatar(String avatar) {this.avatar = avatar;}

  public int getWincounter() {
    return wincounter;
}
public void setWincounter(int wincounter) {
    this.wincounter = wincounter;
}

  
}
