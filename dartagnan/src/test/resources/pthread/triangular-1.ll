; ModuleID = 'triangular-1.c'
source_filename = "triangular-1.c"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

@.str = private unnamed_addr constant [2 x i8] c"0\00", align 1
@.str.1 = private unnamed_addr constant [15 x i8] c"triangular-1.c\00", align 1
@__PRETTY_FUNCTION__.reach_error = private unnamed_addr constant [19 x i8] c"void reach_error()\00", align 1
@i = dso_local global i32 3, align 4
@j = dso_local global i32 6, align 4

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @reach_error() #0 {
  call void @__assert_fail(ptr noundef @.str, ptr noundef @.str.1, i32 noundef 16, ptr noundef @__PRETTY_FUNCTION__.reach_error) #5
  unreachable
}

; Function Attrs: noreturn nounwind
declare void @__assert_fail(ptr noundef, ptr noundef, i32 noundef, ptr noundef) #1

; Function Attrs: noinline nounwind optnone uwtable
define dso_local ptr @t1(ptr noundef %0) #0 {
  %2 = alloca ptr, align 8
  %3 = alloca i32, align 4
  store ptr %0, ptr %2, align 8
  store i32 0, ptr %3, align 4
  br label %4

4:                                                ; preds = %10, %1
  %5 = load i32, ptr %3, align 4
  %6 = icmp slt i32 %5, 5
  br i1 %6, label %7, label %13

7:                                                ; preds = %4
  call void (...) @__VERIFIER_atomic_begin()
  %8 = load i32, ptr @j, align 4
  %9 = add nsw i32 %8, 1
  store i32 %9, ptr @i, align 4
  call void (...) @__VERIFIER_atomic_end()
  br label %10

10:                                               ; preds = %7
  %11 = load i32, ptr %3, align 4
  %12 = add nsw i32 %11, 1
  store i32 %12, ptr %3, align 4
  br label %4, !llvm.loop !6

13:                                               ; preds = %4
  call void @pthread_exit(ptr noundef null) #6
  unreachable
}

declare void @__VERIFIER_atomic_begin(...) #2

declare void @__VERIFIER_atomic_end(...) #2

; Function Attrs: noreturn
declare void @pthread_exit(ptr noundef) #3

; Function Attrs: noinline nounwind optnone uwtable
define dso_local ptr @t2(ptr noundef %0) #0 {
  %2 = alloca ptr, align 8
  %3 = alloca i32, align 4
  store ptr %0, ptr %2, align 8
  store i32 0, ptr %3, align 4
  br label %4

4:                                                ; preds = %10, %1
  %5 = load i32, ptr %3, align 4
  %6 = icmp slt i32 %5, 5
  br i1 %6, label %7, label %13

7:                                                ; preds = %4
  call void (...) @__VERIFIER_atomic_begin()
  %8 = load i32, ptr @i, align 4
  %9 = add nsw i32 %8, 1
  store i32 %9, ptr @j, align 4
  call void (...) @__VERIFIER_atomic_end()
  br label %10

10:                                               ; preds = %7
  %11 = load i32, ptr %3, align 4
  %12 = add nsw i32 %11, 1
  store i32 %12, ptr %3, align 4
  br label %4, !llvm.loop !8

13:                                               ; preds = %4
  call void @pthread_exit(ptr noundef null) #6
  unreachable
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @main(i32 noundef %0, ptr noundef %1) #0 {
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  %5 = alloca ptr, align 8
  %6 = alloca i64, align 8
  %7 = alloca i64, align 8
  %8 = alloca i32, align 4
  %9 = alloca i32, align 4
  store i32 0, ptr %3, align 4
  store i32 %0, ptr %4, align 4
  store ptr %1, ptr %5, align 8
  %10 = call i32 @pthread_create(ptr noundef %6, ptr noundef null, ptr noundef @t1, ptr noundef null) #7
  %11 = call i32 @pthread_create(ptr noundef %7, ptr noundef null, ptr noundef @t2, ptr noundef null) #7
  call void (...) @__VERIFIER_atomic_begin()
  %12 = load i32, ptr @i, align 4
  %13 = icmp sgt i32 %12, 16
  %14 = zext i1 %13 to i32
  store i32 %14, ptr %8, align 4
  call void (...) @__VERIFIER_atomic_end()
  call void (...) @__VERIFIER_atomic_begin()
  %15 = load i32, ptr @j, align 4
  %16 = icmp sgt i32 %15, 16
  %17 = zext i1 %16 to i32
  store i32 %17, ptr %9, align 4
  call void (...) @__VERIFIER_atomic_end()
  %18 = load i32, ptr %8, align 4
  %19 = icmp ne i32 %18, 0
  br i1 %19, label %23, label %20

20:                                               ; preds = %2
  %21 = load i32, ptr %9, align 4
  %22 = icmp ne i32 %21, 0
  br i1 %22, label %23, label %25

23:                                               ; preds = %20, %2
  br label %24

24:                                               ; preds = %23
  call void @reach_error()
  call void @abort() #6
  unreachable

25:                                               ; preds = %20
  ret i32 0
}

; Function Attrs: nounwind
declare i32 @pthread_create(ptr noundef, ptr noundef, ptr noundef, ptr noundef) #4

; Function Attrs: noreturn
declare void @abort() #3

attributes #0 = { noinline nounwind optnone uwtable "frame-pointer"="all" "min-legal-vector-width"="0" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
attributes #1 = { noreturn nounwind "frame-pointer"="all" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
attributes #2 = { "frame-pointer"="all" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
attributes #3 = { noreturn "frame-pointer"="all" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
attributes #4 = { nounwind "frame-pointer"="all" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
attributes #5 = { noreturn nounwind }
attributes #6 = { noreturn }
attributes #7 = { nounwind }

!llvm.module.flags = !{!0, !1, !2, !3, !4}
!llvm.ident = !{!5}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 8, !"PIC Level", i32 2}
!2 = !{i32 7, !"PIE Level", i32 2}
!3 = !{i32 7, !"uwtable", i32 2}
!4 = !{i32 7, !"frame-pointer", i32 2}
!5 = !{!"Ubuntu clang version 18.1.3 (1ubuntu1)"}
!6 = distinct !{!6, !7}
!7 = !{!"llvm.loop.mustprogress"}
!8 = distinct !{!8, !7}
