// 로그인 관련
import api from "../instances/api";

// 회원가입
export const createUser = (email: string, password: string, nickName: string, profileImage: string) =>
  api.post(`auth/signup`, {
    email,
    password,
    nickName,
    profileImage,
  });

// 로그인
export const signinUser = (email: string, password: string) => api.post(`auth/login`, { email, password });

// 유저 정보 조회
export const getUserInfo = () => api.get(`auth/user-info`);

// 게시글 공유
export const shareNote = (userId: number, userList: number[], editorId: string) => {
  const data = { userId, userList, editorId };
  return api.post(`editor/share`, data);
};
// 인물 검색
export const getOtherUserPkByNickName = (nickName: string) => {
  return api.get(`auth/user-info/${nickName}`);
};

// userPk 로 user info 반환
export const getUsersInfo = (userPkList: number[]) => {
  return api.post(`auth/user-info/pk-list`, { userPkList: userPkList });
};
