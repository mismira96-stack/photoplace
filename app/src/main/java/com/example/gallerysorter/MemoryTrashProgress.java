package com.example.gallerysorter;

/** Pure checkpoint transition; only confirmed observations advance cumulative totals. */
final class MemoryTrashProgress {
    static void record(MemoryTrashJournal.Session s,int end,int moved,int skipped,int unknown) {
        int size=s.batch().size(),count=end-s.audited;
        if(!s.state.equals("AUDITING")||count<=0||end>size||moved<0||skipped<0||unknown<0||moved+skipped+unknown!=count)
            throw new IllegalArgumentException("Invalid audit checkpoint");
        s.audited=end;s.batchMoved+=moved;s.batchSkipped+=skipped;s.batchUnknown+=unknown;
        if(end!=size)return;
        if(s.batchUnknown>0) {
            s.state="NEEDS_ATTENTION";s.message="일부 사진 상태를 확인하지 못했어요. 권한과 갤러리 휴지통을 확인해 주세요.";
        } else if(s.batchMoved==0) {
            s.state="PAUSED";s.message="이번 묶음은 이동되지 않았어요. 다시 시도하거나 여기서 중단할 수 있어요.";
            reset(s);
        } else {
            s.offset+=size;s.moved+=s.batchMoved;s.skipped+=s.batchSkipped;reset(s);
            s.state=s.offset==s.rows.size()?"DONE":"PAUSED";
            s.message=s.moved+"개 이동 · "+s.skipped+"개 이동 안 됨"+
                    (s.state.equals("DONE")?"":" · 남은 "+(s.rows.size()-s.offset)+"개");
        }
    }
    private static void reset(MemoryTrashJournal.Session s){s.audited=s.batchMoved=s.batchSkipped=s.batchUnknown=0;}
}
