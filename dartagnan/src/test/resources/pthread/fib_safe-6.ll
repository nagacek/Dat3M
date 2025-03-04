; ModuleID = 'fib_safe-6.c'
source_filename = "fib_safe-6.c"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

@.str = private unnamed_addr constant [2 x i8] c"0\00", align 1
@.str.1 = private unnamed_addr constant [13 x i8] c"./fib_safe.h\00", align 1
@__PRETTY_FUNCTION__.reach_error = private unnamed_addr constant [19 x i8] c"void reach_error()\00", align 1
@p = dso_local global i32 0, align 4
@i = dso_local global i32 0, align 4
@j = dso_local global i32 0, align 4
@q = dso_local global i32 0, align 4
@cur = dso_local global i32 1, align 4
@prev = dso_local global i32 0, align 4
@next = dso_local global i32 0, align 4
@x = dso_local global i32 0, align 4

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @reach_error() #0 {
  call void @__assert_fail(ptr noundef @.str, ptr noundef @.str.1, i32 noundef 14, ptr noundef @__PRETTY_FUNCTION__.reach_error) #5
  unreachable
}

; Function Attrs: noreturn nounwind
declare void @__assert_fail(ptr noundef, ptr noundef, i32 noundef, ptr noundef) #1

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @__VERIFIER_assert(i32 noundef %0) #0 {
  %2 = alloca i32, align 4
  store i32 %0, ptr %2, align 4
  %3 = load i32, ptr %2, align 4
  %4 = icmp ne i32 %3, 0
  br i1 %4, label %7, label %5

5:                                                ; preds = %1
  br label %6

6:                                                ; preds = %5
  call void @reach_error()
  call void @abort() #6
  unreachable

7:                                                ; preds = %1
  ret void
}

; Function Attrs: noreturn
declare void @abort() #2

; Function Attrs: noinline nounwind optnone uwtable
define dso_local ptr @t1(ptr noundef %0) #0 {
  %2 = alloca ptr, align 8
  store ptr %0, ptr %2, align 8
  store i32 0, ptr @p, align 4
  br label %3

3:                                                ; preds = %10, %1
  %4 = load i32, ptr @p, align 4
  %5 = icmp slt i32 %4, 6
  br i1 %5, label %6, label %13

6:                                                ; preds = %3
  call void @__VERIFIER_atomic_begin()
  %7 = load i32, ptr @i, align 4
  %8 = load i32, ptr @j, align 4
  %9 = add nsw i32 %7, %8
  store i32 %9, ptr @i, align 4
  call void @__VERIFIER_atomic_end()
  br label %10

10:                                               ; preds = %6
  %11 = load i32, ptr @p, align 4
  %12 = add nsw i32 %11, 1
  store i32 %12, ptr @p, align 4
  br label %3, !llvm.loop !6

13:                                               ; preds = %3
  ret ptr null
}

declare void @__VERIFIER_atomic_begin() #3

declare void @__VERIFIER_atomic_end() #3

; Function Attrs: noinline nounwind optnone uwtable
define dso_local ptr @t2(ptr noundef %0) #0 {
  %2 = alloca ptr, align 8
  store ptr %0, ptr %2, align 8
  store i32 0, ptr @q, align 4
  br label %3

3:                                                ; preds = %10, %1
  %4 = load i32, ptr @q, align 4
  %5 = icmp slt i32 %4, 6
  br i1 %5, label %6, label %13

6:                                                ; preds = %3
  call void @__VERIFIER_atomic_begin()
  %7 = load i32, ptr @j, align 4
  %8 = load i32, ptr @i, align 4
  %9 = add nsw i32 %7, %8
  store i32 %9, ptr @j, align 4
  call void @__VERIFIER_atomic_end()
  br label %10

10:                                               ; preds = %6
  %11 = load i32, ptr @q, align 4
  %12 = add nsw i32 %11, 1
  store i32 %12, ptr @q, align 4
  br label %3, !llvm.loop !8

13:                                               ; preds = %3
  ret ptr null
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @fib() #0 {
  store i32 0, ptr @x, align 4
  br label %1

1:                                                ; preds = %10, %0
  %2 = load i32, ptr @x, align 4
  %3 = icmp slt i32 %2, 14
  br i1 %3, label %4, label %13

4:                                                ; preds = %1
  %5 = load i32, ptr @prev, align 4
  %6 = load i32, ptr @cur, align 4
  %7 = add nsw i32 %5, %6
  store i32 %7, ptr @next, align 4
  %8 = load i32, ptr @cur, align 4
  store i32 %8, ptr @prev, align 4
  %9 = load i32, ptr @next, align 4
  store i32 %9, ptr @cur, align 4
  br label %10

10:                                               ; preds = %4
  %11 = load i32, ptr @x, align 4
  %12 = add nsw i32 %11, 1
  store i32 %12, ptr @x, align 4
  br label %1, !llvm.loop !9

13:                                               ; preds = %1
  %14 = load i32, ptr @prev, align 4
  ret i32 %14
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @main(i32 noundef %0, ptr noundef %1) #0 {
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  %5 = alloca ptr, align 8
  %6 = alloca i64, align 8
  %7 = alloca i64, align 8
  %8 = alloca i32, align 4
  %9 = alloca i8, align 1
  store i32 0, ptr %3, align 4
  store i32 %0, ptr %4, align 4
  store ptr %1, ptr %5, align 8
  call void @__VERIFIER_atomic_begin()
  store i32 1, ptr @i, align 4
  call void @__VERIFIER_atomic_end()
  call void @__VERIFIER_atomic_begin()
  store i32 1, ptr @j, align 4
  call void @__VERIFIER_atomic_end()
  %10 = call i32 @pthread_create(ptr noundef %6, ptr noundef null, ptr noundef @t1, ptr noundef null) #7
  %11 = call i32 @pthread_create(ptr noundef %7, ptr noundef null, ptr noundef @t2, ptr noundef null) #7
  %12 = call i32 @fib()
  store i32 %12, ptr %8, align 4
  call void @__VERIFIER_atomic_begin()
  %13 = load i32, ptr @i, align 4
  %14 = load i32, ptr %8, align 4
  %15 = icmp sle i32 %13, %14
  br i1 %15, label %16, label %20

16:                                               ; preds = %2
  %17 = load i32, ptr @j, align 4
  %18 = load i32, ptr %8, align 4
  %19 = icmp sle i32 %17, %18
  br label %20

20:                                               ; preds = %16, %2
  %21 = phi i1 [ false, %2 ], [ %19, %16 ]
  %22 = zext i1 %21 to i8
  store i8 %22, ptr %9, align 1
  call void @__VERIFIER_atomic_end()
  %23 = load i8, ptr %9, align 1
  %24 = trunc i8 %23 to i1
  %25 = zext i1 %24 to i32
  call void @__VERIFIER_assert(i32 noundef %25)
  ret i32 0
}

; Function Attrs: nounwind
declare i32 @pthread_create(ptr noundef, ptr noundef, ptr noundef, ptr noundef) #4

attributes #0 = { noinline nounwind optnone uwtable "frame-pointer"="all" "min-legal-vector-width"="0" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
attributes #1 = { noreturn nounwind "frame-pointer"="all" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
attributes #2 = { noreturn "frame-pointer"="all" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
attributes #3 = { "frame-pointer"="all" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cmov,+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
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
!9 = distinct !{!9, !7}
