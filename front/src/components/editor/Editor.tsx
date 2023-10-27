'use client';

import { Editor as NovelEditor } from 'novel';
import { useState, useEffect } from 'react';
import { Editor as Editor$1, JSONContent } from '@tiptap/core';

type Props = {
  id: string; // id를 문자열로 지정
};

export default function Editor({ id }: Props) {
  // editorPage Id
  // const EditorpageId = id;
  const [value, setValue] = useState<JSONContent | undefined>({});
  // const [item, setItem] = useState<JSONContent | undefined>({});
  console.log(id);
  // useEffect(() => {
  //   const local = localStorage.getItem('novel__content');
  //   if (local) {
  //     setItem(JSON.parse(local));
  //   }
  // }, [value]);

  return (
    <>
      <NovelEditor defaultValue={{}} onUpdate={(editor) => setValue(editor?.getJSON())} />
      {/* <NovelEditor defaultValue={{}} disableLocalStorage={true} value={item}/> */}
    </>
  );
}